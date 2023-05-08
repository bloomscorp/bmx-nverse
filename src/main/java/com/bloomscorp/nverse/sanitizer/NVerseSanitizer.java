package com.bloomscorp.nverse.sanitizer;

import com.bloomscorp.hastar.CarrierException;
import com.bloomscorp.hastar.code.ErrorCode;
import com.bloomscorp.nverse.support.Constant;
import com.bloomscorp.pastebox.Pastebox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.web.util.HtmlUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public abstract class NVerseSanitizer<E, R> {

	@Getter
	@AllArgsConstructor
	private static class FieldValueTuple {
		private String field;
		private String value;
	}

	private static final List<Pattern> XSS_INPUT_PATTERNS = new ArrayList<>(Arrays.asList(
		Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
		Pattern.compile("<iframe(.*?)>(.*?)</iframe>", Pattern.CASE_INSENSITIVE),
		Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
		Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
		Pattern.compile("src[\r\n]*=[\r\n]*([^>]+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
		Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
		Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
		Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
		Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
		Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
		Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
		Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
	));

	private String stripXSS(String value) {
		if (!Pastebox.isEmptyString(value))
			for (Pattern pattern : NVerseSanitizer.XSS_INPUT_PATTERNS)
				value = pattern.matcher(value).replaceAll(Constant.BLANK_STRING_VALUE);
		return value;
	}

	private @NotNull String canonicalize(String value) {
		// TODO: bring back ESAPI once it supports Jakarta
//		return NVerseESAPI.encoder().canonicalize(value).replaceAll(
//			Constant.END_OF_STRING_VALUE,
//			Constant.BLANK_STRING_VALUE
//		);
		return value.replaceAll(
			Constant.END_OF_STRING_VALUE,
			Constant.BLANK_STRING_VALUE
		);
	}

//	private String encodeHTMLContent(String value) {
//		return Encode.forHtmlContent(value);
//	}

	private String escapeHTML(String html) {
		return HtmlUtils.htmlEscape(html, "UTF-8");
	}

	private String sanitizeHTML(String html) {
		/*
		 * https://github.com/OWASP/java-html-sanitizer
		 * https://www.javadoc.io/doc/com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer/latest/org/owasp/html/Sanitizers.html
		 * https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/util/HtmlUtils.html
		 * https://mvnrepository.com/artifact/com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer
		 */

		PolicyFactory policy = Sanitizers.FORMATTING
								.and(Sanitizers.BLOCKS)
								.and(Sanitizers.IMAGES)
								.and(Sanitizers.LINKS)
								.and(Sanitizers.STYLES)
								.and(Sanitizers.TABLES);

		// TODO: add custom policy
		
		return policy.sanitize(html);
	}

	protected <O> O sanitizeObject(O object, Class<?> @NotNull ... fieldTypes) throws CarrierException {

		for (Class<?> fieldType : fieldTypes)
			Pastebox
				.getClassFields(object)
				.stream()
				.parallel()
				.filter(field -> {
					try {return Pastebox.fieldHasType(field, fieldType, object);} catch (NoSuchFieldException exception) {
						throw new CarrierException(exception, ErrorCode.MALFORMED_PAYLOAD);
					}
				})
				.map(field -> {
					try {return Pastebox.getFieldGetterMethod(field, object);} catch (NoSuchMethodException | SecurityException exception) {
						throw new CarrierException(exception, ErrorCode.MALFORMED_PAYLOAD);
					}
				})
				.filter(method -> Pastebox.methodHasReturnType(method, fieldType))
				.map(Pastebox::getFieldNameFromGetterMethod)
				.map(field -> {
					try {
						return new FieldValueTuple(
							field,
							(String) Pastebox.getFieldGetterMethod(field, object).invoke(object)
						);
					} catch (
						NoSuchMethodException |
						SecurityException |
						IllegalArgumentException |
						IllegalAccessException |
						InvocationTargetException exception) {
						throw new CarrierException(exception, ErrorCode.MALFORMED_PAYLOAD);
					}
				})
				.forEach(tuple -> {
					try {
						Pastebox
							.getFieldSetterMethod(tuple.getField(), fieldType, object)
							.invoke(
								object,
								this.sanitize(tuple.getValue())
							);
					} catch (
						NoSuchMethodException |
						SecurityException |
						IllegalArgumentException |
						IllegalAccessException |
						InvocationTargetException exception
					) {
						throw new CarrierException(exception, ErrorCode.MALFORMED_PAYLOAD);
					}
				});

		return object;
	}

	protected String sanitize(String value) {
		return Pastebox.isEmptyString(value) ? Constant.BLANK_STRING_VALUE : this.sanitizeHTML(
			this.stripXSS(
				this.canonicalize(value)
			)
		).trim();
	}

	public abstract R getSanitized(E entity);
}
