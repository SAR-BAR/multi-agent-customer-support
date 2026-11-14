package com.sarbar.support.domain.service;

import com.sarbar.support.domain.model.Invoice;
import com.sarbar.support.domain.model.Membership;
import com.sarbar.support.domain.model.MembershipStatus;
import com.sarbar.support.domain.model.Order;
import com.sarbar.support.domain.model.Payment;
import com.sarbar.support.domain.model.Refund;
import com.sarbar.support.domain.model.Shipment;
import com.sarbar.support.domain.port.out.InvoiceRepository;
import com.sarbar.support.domain.port.out.MembershipRepository;
import com.sarbar.support.domain.port.out.OrderRepository;
import com.sarbar.support.domain.port.out.PaymentRepository;
import com.sarbar.support.domain.port.out.RefundRepository;
import com.sarbar.support.domain.port.out.ShipmentRepository;
import com.sarbar.support.domain.record.Facts;
import com.sarbar.support.domain.record.Facts.DeliveryInfo;
import com.sarbar.support.domain.record.Facts.InvoiceLineSummary;
import com.sarbar.support.domain.record.Facts.MembershipInfo;
import com.sarbar.support.domain.record.Facts.OrderSummary;
import com.sarbar.support.domain.record.Facts.PaymentSummary;
import com.sarbar.support.domain.record.Facts.RefundSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class FactsComputer {

  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;
  private final InvoiceRepository invoiceRepository;
  private final ShipmentRepository shipmentRepository;
  private final MembershipRepository membershipRepository;
  private final RefundRepository refundRepository;

  public FactsComputer(
      OrderRepository orderRepository,
      PaymentRepository paymentRepository,
      InvoiceRepository invoiceRepository,
      ShipmentRepository shipmentRepository,
      MembershipRepository membershipRepository,
      RefundRepository refundRepository) {
    this.orderRepository = orderRepository;
    this.paymentRepository = paymentRepository;
    this.invoiceRepository = invoiceRepository;
    this.shipmentRepository = shipmentRepository;
    this.membershipRepository = membershipRepository;
    this.refundRepository = refundRepository;
  }

  public Facts compute(String orderId, Instant now) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

    List<Payment> payments = paymentRepository.findByOrderId(orderId);
    List<Shipment> shipments = shipmentRepository.findByOrderId(orderId);
    List<Refund> priorRefunds = refundRepository.findByOrderId(orderId);
    Membership membership =
        membershipRepository.findActiveByCustomerId(order.customerId()).orElse(null);

    OrderSummary orderSummary =
        new OrderSummary(
            order.id(),
            order.status(),
            order.totalPaise(),
            order.deliveryFeePaise(),
            order.orderedAt());

    List<PaymentSummary> paymentSummaries =
        payments.stream()
            .map(
                p ->
                    new PaymentSummary(p.id(), p.status(), p.amountPaise(), p.method(), p.paidAt()))
            .toList();

    List<InvoiceLineSummary> invoiceLineSummaries = computeInvoiceLines(orderId);
    int invoiceTotalPaise =
        invoiceLineSummaries.stream().mapToInt(InvoiceLineSummary::lineTotalPaise).sum();

    DeliveryInfo deliveryInfo = computeDeliveryInfo(shipments, order, now);

    MembershipInfo membershipInfo =
        membership != null
            ? new MembershipInfo(membership.status(), membership.freeDelivery())
            : new MembershipInfo(MembershipStatus.EXPIRED, false);

    int totalPaid =
        payments.stream()
            .filter(p -> p.status() == com.sarbar.support.domain.model.PaymentStatus.COMPLETED)
            .mapToInt(Payment::amountPaise)
            .sum();
    int totalRefunded = priorRefunds.stream().mapToInt(Refund::amountPaise).sum();
    int refundableAmount = totalPaid - totalRefunded;

    List<RefundSummary> refundSummaries =
        priorRefunds.stream()
            .map(r -> new RefundSummary(r.id(), r.amountPaise(), r.refundedAt()))
            .toList();

    return new Facts(
        orderId,
        orderSummary,
        paymentSummaries,
        invoiceLineSummaries,
        invoiceTotalPaise,
        deliveryInfo,
        membershipInfo,
        Math.max(0, refundableAmount),
        refundSummaries);
  }

  private List<InvoiceLineSummary> computeInvoiceLines(String orderId) {
    Invoice invoice = invoiceRepository.findByOrderId(orderId).orElse(null);
    if (invoice == null) {
      return List.of();
    }
    return invoiceRepository.findLinesByInvoiceId(invoice.id()).stream()
        .map(
            line ->
                new InvoiceLineSummary(
                    line.description(),
                    line.quantity(),
                    line.unitPricePaise(),
                    line.lineTotalPaise()))
        .toList();
  }

  private DeliveryInfo computeDeliveryInfo(List<Shipment> shipments, Order order, Instant now) {
    if (shipments.isEmpty()) {
      return new DeliveryInfo(null, order.expectedDeliveryAt(), null, null, 0);
    }
    Shipment shipment = shipments.getFirst();
    int delayDays = computeDelayDays(order.expectedDeliveryAt(), shipment.deliveredAt(), now);
    return new DeliveryInfo(
        shipment.status(),
        order.expectedDeliveryAt(),
        shipment.shippedAt(),
        shipment.deliveredAt(),
        delayDays);
  }

  static int computeDelayDays(Instant expectedDelivery, Instant actualDelivery, Instant now) {
    if (expectedDelivery == null) {
      return 0;
    }
    Instant reference = actualDelivery != null ? actualDelivery : now;
    long days = Duration.between(expectedDelivery, reference).toDays();
    return (int) Math.max(0, days);
  }
}
