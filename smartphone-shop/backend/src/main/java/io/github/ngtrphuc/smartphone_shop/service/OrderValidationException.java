package io.github.ngtrphuc.smartphone_shop.service;

import io.github.ngtrphuc.smartphone_shop.common.exception.ValidationException;

public class OrderValidationException extends ValidationException {

    public OrderValidationException(String message) {
        super(message);
    }
}
