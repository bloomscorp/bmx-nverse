package com.bloomscorp.nverse.validator;

/**
 * Functional interface defining the contract for NVerse request validators.
 *
 * <p>Implementations inspect an entity of type {@code E} and return a boolean
 * indicating whether the entity satisfies the validation rule. This interface
 * is the common abstraction used by {@link DomainValidator} and any other
 * custom validators plugged into the {@link NVerseValidationAspect}.</p>
 *
 * @param <E> the type of entity to validate (e.g., {@link com.bloomscorp.nverse.NVerseHttpRequestWrapper}).
 */
@FunctionalInterface
public interface NVerseValidator<E> {

	/**
	 * Validates the given entity against the rule implemented by this validator.
	 *
	 * @param entity the entity to validate; must not be {@code null}.
	 * @return {@code true} if the entity passes validation; {@code false} otherwise.
	 */
    boolean validate(E entity);
}
