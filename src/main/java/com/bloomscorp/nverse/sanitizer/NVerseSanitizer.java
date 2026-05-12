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

/**
 * Abstract base class for NVerse input sanitizers, providing a multi-layer XSS
 * protection and HTML sanitization pipeline applicable to individual string values or
 * entire object graphs.
 *
 * <p>The sanitization pipeline applied by {@link #sanitize} consists of three stages in
 * order:</p>
 * <ol>
 *   <li><strong>Canonicalization</strong> — strips null bytes ({@code \0}) from the
 *       input to prevent null-byte injection attacks.</li>
 *   <li><strong>XSS pattern stripping</strong> — removes common XSS attack vectors
 *       using a set of compiled regular expressions: {@code <script>} tags, {@code <iframe>}
 *       tags, inline {@code src=} attributes, {@code eval()}, {@code expression()},
 *       {@code javascript:}, {@code vbscript:}, and {@code onload=} handlers.</li>
 *   <li><strong>OWASP HTML sanitization</strong> — passes the stripped value through the
 *       OWASP Java HTML Sanitizer with a policy that allows standard formatting, block
 *       elements, images, links, styles, and tables while stripping all other tags.</li>
 * </ol>
 *
 * <p>Object-level sanitization is available via {@link #sanitizeObject}, which uses
 * Java reflection to find all fields of the specified types on an object, retrieves
 * their values via getters, sanitizes them, and writes the results back via setters.
 * This avoids the need to manually sanitize every field in a DTO.</p>
 *
 * <p>Subclasses must implement {@link #getSanitized(Object)} to define what input they
 * accept and what sanitized output they produce.</p>
 *
 * @param <E> the type of the entity to sanitize (e.g., {@link com.bloomscorp.nverse.NVerseHttpRequestWrapper}).
 * @param <R> the type of the sanitized result (e.g., {@link String} for raw request dump sanitization).
 */
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

	/**
	 * Sanitizes all string fields of a given type on the provided object in place,
	 * using reflection to read and write values via conventional getter/setter pairs.
	 *
	 * <p>For each specified {@code fieldType}, the method:</p>
	 * <ol>
	 *   <li>Enumerates all fields declared directly on the object's class.</li>
	 *   <li>Filters to those whose declared type matches {@code fieldType}.</li>
	 *   <li>Reads the current value via the field's getter.</li>
	 *   <li>Applies the full {@link #sanitize} pipeline to the value.</li>
	 *   <li>Writes the sanitized value back via the field's setter.</li>
	 * </ol>
	 *
	 * <p>Reflection exceptions (missing getter/setter, access denied, invocation failure)
	 * are wrapped in a {@link CarrierException} with
	 * {@link ErrorCode#MALFORMED_PAYLOAD}.</p>
	 *
	 * @param <O>        the object type.
	 * @param object     the object whose fields are to be sanitized; must not be {@code null}.
	 * @param fieldTypes the field types to sanitize (e.g., {@code String.class}); must not
	 *                   be {@code null}. Fields of other types are skipped.
	 * @return the same {@code object} instance, with sanitized field values written back.
	 * @throws CarrierException if any reflection operation fails during field access or mutation.
	 */
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

	/**
	 * Applies the full three-stage sanitization pipeline to a single string value:
	 * canonicalization, XSS pattern stripping, and OWASP HTML sanitization.
	 *
	 * <p>If the value is {@code null}, empty, or blank, the method returns an empty
	 * string ({@link Constant#BLANK_STRING_VALUE}) immediately without further processing.</p>
	 *
	 * @param value the raw string to sanitize; may be {@code null} or blank.
	 * @return the sanitized string, trimmed of leading/trailing whitespace; never {@code null}.
	 */
	protected String sanitize(String value) {
		return Pastebox.isEmptyString(value) ? Constant.BLANK_STRING_VALUE : this.sanitizeHTML(
			this.stripXSS(
				this.canonicalize(value)
			)
		).trim();
	}

	/**
	 * Sanitizes the given entity and returns the sanitized result.
	 *
	 * <p>Implementing classes define the specific input type {@code E} and output type
	 * {@code R}, and perform the appropriate calls to {@link #sanitize} or
	 * {@link #sanitizeObject} to produce a clean result.</p>
	 *
	 * @param entity the input to sanitize; must not be {@code null}.
	 * @return the sanitized result.
	 */
	public abstract R getSanitized(E entity);
}
