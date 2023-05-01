package com.bloomscorp.nverse.validator;

import com.bloomscorp.nverse.NVerseHttpRequestWrapper;
import com.bloomscorp.nverse.support.Constant;
import com.bloomscorp.pastebox.Pastebox;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
public class DomainValidator implements NVerseValidator<NVerseHttpRequestWrapper> {

	private final List<String> allowedDomains;

	@Override
	public boolean validate(@NotNull NVerseHttpRequestWrapper request) {
		String origin = request.getHeader("origin");
		return !Pastebox.isEmptyString(origin) &&  this.allowedDomains.contains(origin);
	}
}
