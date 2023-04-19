package com.bloomscorp.nverse.validator;

@FunctionalInterface
public interface NVerseValidator<E> {
    boolean validate(E entity);
}
