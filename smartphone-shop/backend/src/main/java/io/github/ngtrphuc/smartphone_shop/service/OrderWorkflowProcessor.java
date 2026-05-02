package io.github.ngtrphuc.smartphone_shop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.github.ngtrphuc.smartphone_shop.event.OrderCreatedEvent;
import io.github.ngtrphuc.smartphone_shop.event.OrderStatusChangedEvent;

@Component
@ConditionalOnProperty(prefix = "app.order.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderWorkflowProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrderWorkflowProcessor.class);

    private final OrderService orderService;
    private final SimulatedPaymentGateway paymentGateway;
    private final EmailSender emailSender;

    public OrderWorkflowProcessor(
            OrderService orderService,
            SimulatedPaymentGateway paymentGateway,
            EmailSender emailSender) {
        this.orderService = orderService;
        this.paymentGateway = paymentGateway;
        this.emailSender = emailSender;
    }

    @Async("orderWorkflowExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (event == null || event.orderId() == null) {
            return;
        }

        log.info("[OrderWorkflow] started orderId={} code={} user={} amount={}",
                event.orderId(),
                event.orderCode(),
                event.userEmail(),
                event.totalAmount());

        if (!authorizePayment(event)) {
            queuePaymentFailureNotification(event);
            log.info("[OrderWorkflow] finished with payment failure orderId={} code={}",
                    event.orderId(), event.orderCode());
            return;
        }
        reserveFulfillmentSlot(event);
        queueNotification(event);

        log.info("[OrderWorkflow] finished orderId={} code={}", event.orderId(), event.orderCode());
    }

    private boolean authorizePayment(OrderCreatedEvent event) {
        int maxAttempts = paymentGateway.maxAuthorizeAttempts();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            SimulatedPaymentGateway.PaymentAuthorizationResult result = paymentGateway.authorize(event);
            switch (result.status()) {
                case APPROVED -> {
                    log.info(
                            "[OrderWorkflow] payment-authorized orderId={} paymentMethod={} paymentPlan={} gatewayRef={} attempt={}",
                            event.orderId(),
                            event.paymentMethod(),
                            event.paymentPlan(),
                            result.gatewayReference(),
                            attempt);
                    safelyUpdateOrderStatus(event.orderId(), "processing");
                    return true;
                }
                case RETRYABLE_FAILURE -> log.warn(
                        "[OrderWorkflow] payment-retryable-failure orderId={} gatewayRef={} attempt={}/{} reason={}",
                        event.orderId(),
                        result.gatewayReference(),
                        attempt,
                        maxAttempts,
                        result.reason());
                case DECLINED -> {
                    log.warn(
                            "[OrderWorkflow] payment-declined orderId={} gatewayRef={} reason={}",
                            event.orderId(),
                            result.gatewayReference(),
                            result.reason());
                    safelyUpdateOrderStatus(event.orderId(), "cancelled");
                    return false;
                }
            }
        }

        log.error("[OrderWorkflow] payment-authorization-exhausted orderId={} maxAttempts={}",
                event.orderId(), maxAttempts);
        safelyUpdateOrderStatus(event.orderId(), "cancelled");
        return false;
    }

    private void reserveFulfillmentSlot(OrderCreatedEvent event) {
        log.info("[OrderWorkflow] fulfillment-queued orderId={} itemCount={}",
                event.orderId(),
                event.itemCount());
    }

    private void queueNotification(OrderCreatedEvent event) {
        emailSender.sendOrderConfirmation(event.userEmail(), event.orderCode(), event.totalAmount());
        log.info("[OrderWorkflow] notification-queued orderId={} user={}",
                event.orderId(),
                event.userEmail());
    }

    private void queuePaymentFailureNotification(OrderCreatedEvent event) {
        log.info("[OrderWorkflow] payment-failure-notification-queued orderId={} user={}",
                event.orderId(),
                event.userEmail());
    }

    private void safelyUpdateOrderStatus(Long orderId, String targetStatus) {
        try {
            orderService.updateStatus(orderId, targetStatus);
        } catch (RuntimeException ex) {
            log.warn("[OrderWorkflow] failed to update order status orderId={} targetStatus={} cause={}",
                    orderId,
                    targetStatus,
                    ex.getMessage());
        }
    }

    @Async("orderWorkflowExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event == null || event.orderId() == null) {
            return;
        }
        emailSender.sendOrderStatusUpdate(
                event.userEmail(),
                event.orderCode(),
                event.oldStatus(),
                event.newStatus(),
                event.trackingNumber(),
                event.trackingCarrier());
    }
}
