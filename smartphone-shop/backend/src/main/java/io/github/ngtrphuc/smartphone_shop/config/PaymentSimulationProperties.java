package io.github.ngtrphuc.smartphone_shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.order.payment-simulation")
public class PaymentSimulationProperties {

    private boolean enabled = true;
    private int retryableFailurePercent = 12;
    private int declinePercent = 5;
    private int maxAuthorizeAttempts = 3;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRetryableFailurePercent() {
        return clampPercent(retryableFailurePercent);
    }

    public void setRetryableFailurePercent(int retryableFailurePercent) {
        this.retryableFailurePercent = retryableFailurePercent;
    }

    public int getDeclinePercent() {
        return clampPercent(declinePercent);
    }

    public void setDeclinePercent(int declinePercent) {
        this.declinePercent = declinePercent;
    }

    public int getMaxAuthorizeAttempts() {
        return Math.max(1, maxAuthorizeAttempts);
    }

    public void setMaxAuthorizeAttempts(int maxAuthorizeAttempts) {
        this.maxAuthorizeAttempts = maxAuthorizeAttempts;
    }

    private int clampPercent(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(100, value);
    }
}
