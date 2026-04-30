package io.github.ngtrphuc.smartphone_shop.common.support;

import java.util.regex.Pattern;

public final class ValidationConstants {

    public static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{6,30}$");

    private ValidationConstants() {
    }
}
