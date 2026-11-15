package com.sarbar.support.config;

import com.sarbar.support.domain.port.out.ApprovedAnswerRepository;
import com.sarbar.support.domain.port.out.BillingAgent;
import com.sarbar.support.domain.port.out.CasePostRepository;
import com.sarbar.support.domain.port.out.CaseRepository;
import com.sarbar.support.domain.port.out.CommunicatorAgent;
import com.sarbar.support.domain.port.out.EscalationRepository;
import com.sarbar.support.domain.port.out.InvoiceRepository;
import com.sarbar.support.domain.port.out.MembershipRepository;
import com.sarbar.support.domain.port.out.OrderRepository;
import com.sarbar.support.domain.port.out.PaymentRepository;
import com.sarbar.support.domain.port.out.RefundRepository;
import com.sarbar.support.domain.port.out.ResolutionAgent;
import com.sarbar.support.domain.port.out.RiskAgent;
import com.sarbar.support.domain.port.out.ShipmentRepository;
import com.sarbar.support.domain.port.out.TriageAgent;
import com.sarbar.support.domain.service.CaseBoardService;
import com.sarbar.support.domain.service.CaseProcessor;
import com.sarbar.support.domain.service.DecisionEngine;
import com.sarbar.support.domain.service.FactsComputer;
import com.sarbar.support.domain.service.GeneralHandler;
import com.sarbar.support.domain.service.Referee;
import com.sarbar.support.domain.service.ScopedBoardView;
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

  @Bean
  public ScopedBoardView scopedBoardView(CasePostRepository casePostRepository) {
    return new ScopedBoardView(casePostRepository);
  }

  @Bean
  public CaseProcessor caseProcessor(
      TriageAgent triageAgent,
      ResolutionAgent resolutionAgent,
      BillingAgent billingAgent,
      RiskAgent riskAgent,
      CommunicatorAgent communicatorAgent,
      CaseBoardService caseBoardService,
      ScopedBoardView scopedBoardView,
      FactsComputer factsComputer,
      Referee referee,
      DecisionEngine decisionEngine,
      GeneralHandler generalHandler,
      CaseRepository caseRepository,
      EscalationRepository escalationRepository) {
    return new CaseProcessor(
        triageAgent,
        resolutionAgent,
        billingAgent,
        riskAgent,
        communicatorAgent,
        caseBoardService,
        scopedBoardView,
        factsComputer,
        referee,
        decisionEngine,
        generalHandler,
        caseRepository,
        escalationRepository);
  }
}
