package io.github.ngtrphuc.smartphone_shop.api;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.ngtrphuc.smartphone_shop.service.OrderValidationException;

@RestControllerAdvice(basePackages = "io.github.ngtrphuc.smartphone_shop.controller.api")
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiDtos.ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiDtos.ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(OrderValidationException.class)
    public ResponseEntity<ApiDtos.ErrorResponse> handleOrderValidation(OrderValidationException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiDtos.ErrorResponse("ORDER_VALIDATION_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiDtos.ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiDtos.ErrorResponse("CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiDtos.ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiDtos.ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiDtos.ErrorResponse> handleUnreadableBody() {
        return ResponseEntity.badRequest()
                .body(new ApiDtos.ErrorResponse("INVALID_BODY", "Request body is missing or invalid."));
    }
}
