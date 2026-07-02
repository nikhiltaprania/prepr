package com.springrag.common.web;

import com.springrag.common.exception.DuplicateResourceException;
import com.springrag.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }

    @Test
    void mapsResourceNotFoundTo404() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException("User not found: 1"), request("/api/users/1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("User not found: 1");
        assertThat(response.getBody().getPath()).isEqualTo("/api/users/1");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void mapsDuplicateResourceTo409() {
        ResponseEntity<ApiError> response =
                handler.handleDuplicate(new DuplicateResourceException("Email already exists: a@b.com"), request("/api/users"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    void mapsIllegalArgumentTo400() {
        ResponseEntity<ApiError> response =
                handler.handleBadRequest(new IllegalArgumentException("bad input"), request("/api/search"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("bad input");
    }

    @Test
    void mapsUnexpectedTo500WithGenericMessage() {
        ResponseEntity<ApiError> response =
                handler.handleUnexpected(new RuntimeException("leaky detail"), request("/api/chat"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
