package com.sarbar.support.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sarbar.support.domain.model.Invoice;
import com.sarbar.support.domain.model.InvoiceLine;
import com.sarbar.support.domain.model.Membership;
import com.sarbar.support.domain.model.MembershipStatus;
import com.sarbar.support.domain.model.Order;
import com.sarbar.support.domain.model.OrderStatus;
import com.sarbar.support.domain.model.Payment;
import com.sarbar.support.domain.model.PaymentStatus;
import com.sarbar.support.domain.model.Refund;
import com.sarbar.support.domain.model.Shipment;
import com.sarbar.support.domain.model.ShipmentStatus;
import com.sarbar.support.domain.port.out.InvoiceRepository;
import com.sarbar.support.domain.port.out.MembershipRepository;
import com.sarbar.support.domain.port.out.OrderRepository;
import com.sarbar.support.domain.port.out.PaymentRepository;
import com.sarbar.support.domain.port.out.RefundRepository;
import com.sarbar.support.domain.port.out.ShipmentRepository;
import com.sarbar.support.domain.record.Facts;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FactsComputerTest {

  private StubOrderRepository orderRepo;
  private StubPaymentRepository paymentRepo;
  private StubInvoiceRepository invoiceRepo;
  private StubShipmentRepository shipmentRepo;
  private StubMembershipRepository membershipRepo;
  private StubRefundRepository refundRepo;
  private FactsComputer computer;

  private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

  @BeforeEach
  void setUp() {
    orderRepo = new StubOrderRepository();
    paymentRepo = new StubPaymentRepository();
    invoiceRepo = new StubInvoiceRepository();
    shipmentRepo = new StubShipmentRepository();
    membershipRepo = new StubMembershipRepository();
    refundRepo = new StubRefundRepository();
    computer =
        new FactsComputer(
            orderRepo, paymentRepo, invoiceRepo, shipmentRepo, membershipRepo, refundRepo);
  }

  @Test
  void compute_lateDelivery_calculatesDelayDays() {
    Instant expectedDelivery = NOW.minus(5, ChronoUnit.DAYS);
    orderRepo.add(order("ORD-001", 1L, OrderStatus.SHIPPED, 250000, 5000, expectedDelivery));
    paymentRepo.add(payment("ORD-001", PaymentStatus.COMPLETED, 250000));
    shipmentRepo.add(shipment("ORD-001", ShipmentStatus.IN_TRANSIT, null));

    Facts facts = computer.compute("ORD-001", NOW);

    assertThat(facts.delivery().delayDays()).isEqualTo(5);
    assertThat(facts.delivery().status()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    assertThat(facts.refundableAmountPaise()).isEqualTo(250000);
  }

  @Test
  void compute_doubleCharge_showsTwoPayments() {
    orderRepo.add(order("ORD-002", 1L, OrderStatus.DELIVERED, 180000, 0, null));
    paymentRepo.add(payment("ORD-002", PaymentStatus.COMPLETED, 180000));
    paymentRepo.add(payment("ORD-002", PaymentStatus.COMPLETED, 180000));
    shipmentRepo.add(shipment("ORD-002", ShipmentStatus.DELIVERED, NOW.minus(10, ChronoUnit.DAYS)));

    Facts facts = computer.compute("ORD-002", NOW);

    assertThat(facts.payments()).hasSize(2);
    assertThat(facts.refundableAmountPaise()).isEqualTo(360000);
  }

  @Test
  void compute_withPriorRefund_reducesRefundableAmount() {
    orderRepo.add(order("ORD-002", 1L, OrderStatus.DELIVERED, 180000, 0, null));
    paymentRepo.add(payment("ORD-002", PaymentStatus.COMPLETED, 180000));
    paymentRepo.add(payment("ORD-002", PaymentStatus.COMPLETED, 180000));
    refundRepo.add(new Refund(1L, "ORD-002", 180000, "key-1", NOW.minus(1, ChronoUnit.DAYS)));

    Facts facts = computer.compute("ORD-002", NOW);

    assertThat(facts.refundableAmountPaise()).isEqualTo(180000);
    assertThat(facts.priorRefunds()).hasSize(1);
  }

  @Test
  void compute_activeMembership_showsFreeDelivery() {
    orderRepo.add(order("ORD-003", 2L, OrderStatus.DELIVERED, 75000, 5000, null));
    paymentRepo.add(payment("ORD-003", PaymentStatus.COMPLETED, 75000));
    membershipRepo.set(
        new Membership(
            1L, 2L, MembershipStatus.ACTIVE, true, NOW.minus(180, ChronoUnit.DAYS), null));
    shipmentRepo.add(shipment("ORD-003", ShipmentStatus.DELIVERED, NOW.minus(2, ChronoUnit.DAYS)));

    Facts facts = computer.compute("ORD-003", NOW);

    assertThat(facts.membership().status()).isEqualTo(MembershipStatus.ACTIVE);
    assertThat(facts.membership().freeDelivery()).isTrue();
    assertThat(facts.order().deliveryFeePaise()).isEqualTo(5000);
  }

  @Test
  void compute_failedOrder_withCompletedPayment() {
    orderRepo.add(order("ORD-004", 2L, OrderStatus.FAILED, 320000, 0, null));
    paymentRepo.add(payment("ORD-004", PaymentStatus.COMPLETED, 320000));

    Facts facts = computer.compute("ORD-004", NOW);

    assertThat(facts.order().status()).isEqualTo(OrderStatus.FAILED);
    assertThat(facts.refundableAmountPaise()).isEqualTo(320000);
    assertThat(facts.delivery().delayDays()).isEqualTo(0);
  }

  @Test
  void compute_withInvoiceLines_calculatesTotal() {
    orderRepo.add(order("ORD-001", 1L, OrderStatus.SHIPPED, 250000, 5000, null));
    paymentRepo.add(payment("ORD-001", PaymentStatus.COMPLETED, 250000));
    invoiceRepo.set(
        new Invoice(1L, "ORD-001", 250000, NOW),
        List.of(
            new InvoiceLine(1L, 1L, "Headphones", 1, 245000, 245000),
            new InvoiceLine(2L, 1L, "Delivery fee", 1, 5000, 5000)));

    Facts facts = computer.compute("ORD-001", NOW);

    assertThat(facts.invoiceLines()).hasSize(2);
    assertThat(facts.invoiceTotalPaise()).isEqualTo(250000);
  }

  @Test
  void compute_unknownOrder_throws() {
    assertThatThrownBy(() -> computer.compute("NOPE", NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Order not found");
  }

  @Test
  void compute_noMembership_showsExpiredFalse() {
    orderRepo.add(order("ORD-001", 1L, OrderStatus.SHIPPED, 250000, 0, null));
    paymentRepo.add(payment("ORD-001", PaymentStatus.COMPLETED, 250000));

    Facts facts = computer.compute("ORD-001", NOW);

    assertThat(facts.membership().status()).isEqualTo(MembershipStatus.EXPIRED);
    assertThat(facts.membership().freeDelivery()).isFalse();
  }

  @Test
  void computeDelayDays_noDelay_returnsZero() {
    Instant expected = NOW.plus(2, ChronoUnit.DAYS);
    assertThat(FactsComputer.computeDelayDays(expected, null, NOW)).isEqualTo(0);
  }

  @Test
  void computeDelayDays_deliveredLate_usesActualDate() {
    Instant expected = NOW.minus(10, ChronoUnit.DAYS);
    Instant delivered = NOW.minus(3, ChronoUnit.DAYS);
    assertThat(FactsComputer.computeDelayDays(expected, delivered, NOW)).isEqualTo(7);
  }

  // --- Stubs ---

  private static Order order(
      String id,
      Long customerId,
      OrderStatus status,
      int totalPaise,
      int deliveryFeePaise,
      Instant expectedDelivery) {
    return new Order(
        id,
        customerId,
        status,
        totalPaise,
        deliveryFeePaise,
        NOW.minus(30, ChronoUnit.DAYS),
        expectedDelivery,
        0L);
  }

  private static Payment payment(String orderId, PaymentStatus status, int amountPaise) {
    return new Payment(null, orderId, status, amountPaise, "UPI", "TXN-X", NOW);
  }

  private static Shipment shipment(String orderId, ShipmentStatus status, Instant deliveredAt) {
    return new Shipment(
        null, orderId, status, "BlueDart", "BD123", NOW.minus(20, ChronoUnit.DAYS), deliveredAt);
  }

  static class StubOrderRepository implements OrderRepository {
    private final List<Order> orders = new ArrayList<>();

    void add(Order o) {
      orders.add(o);
    }

    @Override
    public Optional<Order> findById(String id) {
      return orders.stream().filter(o -> o.id().equals(id)).findFirst();
    }

    @Override
    public List<Order> findByCustomerId(Long customerId) {
      return orders.stream().filter(o -> o.customerId().equals(customerId)).toList();
    }
  }

  static class StubPaymentRepository implements PaymentRepository {
    private final List<Payment> payments = new ArrayList<>();

    void add(Payment p) {
      payments.add(p);
    }

    @Override
    public List<Payment> findByOrderId(String orderId) {
      return payments.stream().filter(p -> p.orderId().equals(orderId)).toList();
    }
  }

  static class StubInvoiceRepository implements InvoiceRepository {
    private Invoice invoice;
    private List<InvoiceLine> lines = List.of();

    void set(Invoice inv, List<InvoiceLine> invoiceLines) {
      this.invoice = inv;
      this.lines = invoiceLines;
    }

    @Override
    public Optional<Invoice> findByOrderId(String orderId) {
      return invoice != null && invoice.orderId().equals(orderId)
          ? Optional.of(invoice)
          : Optional.empty();
    }

    @Override
    public List<InvoiceLine> findLinesByInvoiceId(Long invoiceId) {
      return lines;
    }
  }

  static class StubShipmentRepository implements ShipmentRepository {
    private final List<Shipment> shipments = new ArrayList<>();

    void add(Shipment s) {
      shipments.add(s);
    }

    @Override
    public List<Shipment> findByOrderId(String orderId) {
      return shipments.stream().filter(s -> s.orderId().equals(orderId)).toList();
    }
  }

  static class StubMembershipRepository implements MembershipRepository {
    private Membership membership;

    void set(Membership m) {
      this.membership = m;
    }

    @Override
    public Optional<Membership> findActiveByCustomerId(Long customerId) {
      return membership != null && membership.customerId().equals(customerId)
          ? Optional.of(membership)
          : Optional.empty();
    }
  }

  static class StubRefundRepository implements RefundRepository {
    private final List<Refund> refunds = new ArrayList<>();

    void add(Refund r) {
      refunds.add(r);
    }

    @Override
    public Refund save(Refund r) {
      refunds.add(r);
      return r;
    }

    @Override
    public List<Refund> findByOrderId(String orderId) {
      return refunds.stream().filter(r -> r.orderId().equals(orderId)).toList();
    }

    @Override
    public boolean existsByIdempotencyKey(String key) {
      return refunds.stream().anyMatch(r -> r.idempotencyKey().equals(key));
    }
  }
}
