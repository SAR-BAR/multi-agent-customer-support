package com.sarbar.support.config;

import com.sarbar.support.domain.port.out.ApprovedAnswerRepository;
import com.sarbar.support.domain.port.out.CasePostRepository;
import com.sarbar.support.domain.port.out.InvoiceRepository;
import com.sarbar.support.domain.port.out.MembershipRepository;
import com.sarbar.support.domain.port.out.OrderRepository;
import com.sarbar.support.domain.port.out.PaymentRepository;
import com.sarbar.support.domain.port.out.RefundRepository;
import com.sarbar.support.domain.port.out.ShipmentRepository;
import com.sarbar.support.domain.service.CaseBoardService;
import com.sarbar.support.domain.service.DecisionEngine;
import com.sarbar.support.domain.service.FactsComputer;
import com.sarbar.support.domain.service.GeneralHandler;
import com.sarbar.support.domain.service.Referee;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

  @Bean
  public CaseBoardService caseBoardService(CasePostRepository casePostRepository) {
    return new CaseBoardService(casePostRepository);
  }

  @Bean
  public FactsComputer factsComputer(
      OrderRepository orderRepository,
      PaymentRepository paymentRepository,
      InvoiceRepository invoiceRepository,
      ShipmentRepository shipmentRepository,
      MembershipRepository membershipRepository,
      RefundRepository refundRepository) {
    return new FactsComputer(
        orderRepository,
        paymentRepository,
        invoiceRepository,
        shipmentRepository,
        membershipRepository,
        refundRepository);
  }

  @Bean
  public Referee referee() {
    return new Referee();
  }

  @Bean
  public DecisionEngine decisionEngine() {
    return new DecisionEngine();
  }

  @Bean
  public GeneralHandler generalHandler(ApprovedAnswerRepository approvedAnswerRepository) {
    return new GeneralHandler(approvedAnswerRepository);
  }
}
