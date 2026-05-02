package io.github.ngtrphuc.smartphone_shop.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.github.ngtrphuc.smartphone_shop.common.support.StorefrontSupport;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user_created", columnList = "user_email,created_at"),
        @Index(name = "idx_orders_status_created", columnList = "status,created_at")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 100)
    private String userEmail;

    @Column(length = 120)
    private String customerName;

    @Column(length = 30)
    private String phoneNumber;

    @Column(length = 255)
    private String shippingAddress;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "payment_method", nullable = false, length = 40)
    private String paymentMethod = "CASH_ON_DELIVERY";

    @Column(name = "payment_detail", length = 200)
    private String paymentDetail;

    @Column(name = "payment_plan", length = 20)
    private String paymentPlan = "FULL_PAYMENT";

    @Column(name = "installment_months")
    private Integer installmentMonths;

    @Column(name = "installment_monthly_amount")
    private Long installmentMonthlyAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "tracking_carrier", length = 50)
    private String trackingCarrier;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentDetail() {
        return paymentDetail;
    }

    public void setPaymentDetail(String paymentDetail) {
        this.paymentDetail = paymentDetail;
    }

    public String getPaymentPlan() {
        return paymentPlan;
    }

    public void setPaymentPlan(String paymentPlan) {
        this.paymentPlan = paymentPlan;
    }

    public Integer getInstallmentMonths() {
        return installmentMonths;
    }

    public void setInstallmentMonths(Integer installmentMonths) {
        this.installmentMonths = installmentMonths;
    }

    public Long getInstallmentMonthlyAmount() {
        return installmentMonthlyAmount;
    }

    public void setInstallmentMonthlyAmount(Long installmentMonthlyAmount) {
        this.installmentMonthlyAmount = installmentMonthlyAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getTrackingCarrier() {
        return trackingCarrier;
    }

    public void setTrackingCarrier(String trackingCarrier) {
        this.trackingCarrier = trackingCarrier;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getPaymentMethodDisplayName() {
        return StorefrontSupport.paymentDisplayName(paymentMethod, paymentDetail);
    }

    public boolean isInstallment() {
        return "INSTALLMENT".equalsIgnoreCase(paymentPlan);
    }

    public String getPaymentPlanDisplayName() {
        return isInstallment() ? "Installment" : "Full payment";
    }

    public String getOrderCode() {
        return StorefrontSupport.orderCode(id);
    }

    public int getItemCount() {
        return items.stream()
                .mapToInt(item -> item != null ? item.getQuantity() : 0)
                .sum();
    }

    public boolean isCancelable() {
        try {
            return OrderStatus.from(status).isCancelableByCustomer();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public String getStatusSummary() {
        try {
            return switch (OrderStatus.from(status)) {
                case PENDING -> "Awaiting store confirmation.";
                case PROCESSING -> "Your order is being prepared for shipment.";
                case SHIPPED -> "Your package is on the way.";
                case DELIVERED -> "Delivered successfully.";
                case COMPLETED -> "Order completed.";
                case CANCELLED -> "This order was cancelled.";
                case RETURN_REQUESTED -> "Return request submitted and pending review.";
                case RETURN_APPROVED -> "Return approved. Refund is being processed.";
                case RETURN_REJECTED -> "Return request was rejected.";
                case REFUNDED -> "Refund completed.";
            };
        } catch (IllegalArgumentException ex) {
            return "Order update pending.";
        }
    }
}

