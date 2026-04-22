package io.github.ngtrphuc.smartphone_shop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.github.ngtrphuc.smartphone_shop.event.OrderCreatedEvent;

@Component
@ConditionalOnProperty(prefix = "app.order.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderWorkflowProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrderWorkflowProcessor.class);

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

        authorizePayment(event);
        reserveFulfillmentSlot(event);
        queueNotification(event);

        log.info("[OrderWorkflow] finished orderId={} code={}", event.orderId(), event.orderCode());
    }

    private void authorizePayment(OrderCreatedEvent event) {
        log.info("[OrderWorkflow] payment-authorized orderId={} paymentMethod={} paymentPlan={}",
                event.orderId(),
                event.paymentMethod(),
                event.paymentPlan());
    }

    private void reserveFulfillmentSlot(OrderCreatedEvent event) {
        log.info("[OrderWorkflow] fulfillment-queued orderId={} itemCount={}",
                event.orderId(),
                event.itemCount());
    }

    private void queueNotification(OrderCreatedEvent event) {
        log.info("[OrderWorkflow] notification-queued orderId={} user={}",
                event.orderId(),
                event.userEmail());
    }
}
