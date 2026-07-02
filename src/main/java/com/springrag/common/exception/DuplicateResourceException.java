package com.springrag.common.exception;

import com.springrag.common.web.GlobalExceptionHandler;

/**
 * Thrown when creating a resource that violates a uniqueness constraint
 * (for example, a user email that already exists). Mapped to HTTP 409 by
 * {@link GlobalExceptionHandler}.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
