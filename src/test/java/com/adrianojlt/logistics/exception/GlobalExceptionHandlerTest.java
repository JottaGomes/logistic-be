package com.adrianojlt.logistics.exception;

import com.adrianojlt.logistics.dto.ApiResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_returnsBadRequestWithFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(
                List.of(new FieldError("request", "income", "must not be null")));

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("income: must not be null");
    }

    @Test
    void handleValidation_withMultipleErrors_joinsAllMessages() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "income", "must not be null"),
                new FieldError("request", "cost", "must not be null")));

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().message())
                .contains("income: must not be null")
                .contains("cost: must not be null");
    }

    @Test
    void handleIllegalArgument_returnsBadRequestWithExceptionMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("invalid shipment data");

        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("invalid shipment data");
    }

    @Test
    void handleGeneric_returnsInternalServerError() {
        Exception ex = new RuntimeException("unexpected failure");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Internal server error");
        assertThat(response.getBody().data()).isNull();
    }
}
