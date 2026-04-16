package io.github.ngtrphuc.smartphone_shop.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PaymentMethodTest {

    @Test
    void getMaskedDetail_shouldReturnNullForBlank() {
        PaymentMethod method = new PaymentMethod();
        method.setDetail("   ");

        assertNull(method.getMaskedDetail());
    }

    @Test
    void getMaskedDetail_shouldReturnMaskedValueForShortInput() {
        PaymentMethod method = new PaymentMethod();
        method.setDetail("1234");

        assertEquals("****", method.getMaskedDetail());
    }

    @Test
    void getMaskedDetail_shouldNormalizeWhitespaceAndShowLast4() {
        PaymentMethod method = new PaymentMethod();
        method.setDetail("Mizuho 1234 5678");

        assertEquals("****5678", method.getMaskedDetail());
    }

    @Test
    void getDisplayName_shouldKeepVisaSeparateFromMasterCard() {
        PaymentMethod method = new PaymentMethod();
        method.setType(PaymentMethod.Type.VISA);

        assertEquals("Visa", method.getDisplayName());
    }
}
