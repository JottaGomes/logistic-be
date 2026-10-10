package com.jgomes.logistics.exception;

import com.jgomes.logistics.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleShipmentNotFound(ShipmentNotFoundException ex) {
        log.warn("shipment_not_found message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateShipmentException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateShipment(DuplicateShipmentException ex) {
        log.warn("duplicate_shipment message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AmountLineNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAmountLineNotFound(AmountLineNotFoundException ex) {
        log.warn("amount_line_not_found message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * A correction based on a version that is no longer current.
     * OptimisticLockingFailureException is also a DataAccessException; Spring picks
     * the most specific handler, so a stale update becomes a 409 telling the caller
     * to reload, not the 503 that asks them to retry the same request unchanged.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleStaleUpdate(OptimisticLockingFailureException ex) {
        log.warn("stale_update message={}", ex.getMessage());
        String message = ex instanceof StaleAmountLineException
                ? ex.getMessage()
                : "The record was changed by someone else. Reload it and try again.";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * A body that is not valid JSON, or does not fit the request object, is the
     * caller's mistake; without this handler it would fall through to the catch-all
     * and be reported as a server error.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Malformed request body"));
    }

    /** A query or path parameter of the wrong type, such as {@code size=abc}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid value for parameter '" + ex.getName() + "'"));
    }

    /**
     * A known path called with a verb it does not support, for example a POST to
     * /api/shipments/{reference}. Answered with 405 and the Allow header rather
     * than a 500 from the catch-all.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        Set<HttpMethod> supported = ex.getSupportedHttpMethods();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(supported == null ? new HttpMethod[0] : supported.toArray(HttpMethod[]::new))
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Alternative flow I of the Calculate Profit use case, Data Retrieval Error: the
     * income or cost records could not be read. The cause is logged in full, and the
     * caller receives 503 rather than 500 because the request itself was valid and
     * the Finance Department is expected to retry it once the database is reachable.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        log.error("data_retrieval_error", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Could not retrieve the income and cost data. Please retry the request."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("unexpected_error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Internal server error"));
    }
}
