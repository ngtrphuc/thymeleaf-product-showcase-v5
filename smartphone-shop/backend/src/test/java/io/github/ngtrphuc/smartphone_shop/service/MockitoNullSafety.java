package io.github.ngtrphuc.smartphone_shop.service;

import static org.mockito.ArgumentMatchers.any;

import java.util.Objects;

import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.lang.NonNull;

final class MockitoNullSafety {

    private MockitoNullSafety() {
    }

    @NonNull
    static <T> T anyNonNull(Class<T> type) {
        return any(type);
    }

    @NonNull
    static <T> T captureNonNull(ArgumentCaptor<T> captor) {
        return captor.capture();
    }

    @NonNull
    static <T> T capturedValue(ArgumentCaptor<T> captor) {
        return Objects.requireNonNull(captor.getValue());
    }

    @NonNull
    static <T> Iterable<T> nonNullIterable(Iterable<T> iterable) {
        return Objects.requireNonNull(iterable);
    }

    @NonNull
    static <T> Answer<T> returnsFirstArgument(Class<T> type) {
        return invocation -> Objects.requireNonNull(invocation.getArgument(0, type));
    }
}
