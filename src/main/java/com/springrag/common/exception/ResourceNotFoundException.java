package com.springrag.common.exception;

import com.springrag.common.web.GlobalExceptionHandler;

/**
 * Thrown when a requested domain resource (user, conversation, document, knowledge base, ...)
 * does not exist. Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
