package com.sarbar.support.domain.record;

import com.sarbar.support.domain.model.MembershipStatus;
import com.sarbar.support.domain.model.OrderStatus;
import com.sarbar.support.domain.model.PaymentStatus;
import com.sarbar.support.domain.model.ShipmentStatus;
import java.time.Instant;
import java.util.List;

public record Facts(
    String orderId,
    OrderSummary order,
    List<PaymentSummary> payments,
    List<InvoiceLineSummary> invoiceLines,
    int invoiceTotalPaise,
    DeliveryInfo delivery,
    MembershipInfo membership,
    int refundableAmountPaise,
    List<RefundSummary> priorRefunds) {

  public record OrderSummary(
      String id, OrderStatus status, int totalPaise, int deliveryFeePaise, Instant orderedAt) {}

  public record PaymentSummary(
      Long id, PaymentStatus status, int amountPaise, String method, Instant paidAt) {}

  public record InvoiceLineSummary(
      String description, int quantity, int unitPricePaise, int lineTotalPaise) {}

  public record DeliveryInfo(
      ShipmentStatus status,
      Instant expectedDeliveryAt,
      Instant shippedAt,
      Instant deliveredAt,
      int delayDays) {}

  public record MembershipInfo(MembershipStatus status, boolean freeDelivery) {}

  public record RefundSummary(Long id, int amountPaise, Instant refundedAt) {}
}
