package com.sarbar.support.config;

import com.sarbar.support.adapter.out.agent.llm.BillingAssistant;
import com.sarbar.support.adapter.out.agent.llm.CommunicatorAssistant;
import com.sarbar.support.adapter.out.agent.llm.ResolutionAssistant;
import com.sarbar.support.adapter.out.agent.llm.RiskAssistant;
import com.sarbar.support.adapter.out.agent.llm.TriageAssistant;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LlmAgentConfig {

  @Bean
  public TriageAssistant triageAssistant(ChatModel chatModel) {
    return AiServices.builder(TriageAssistant.class).chatModel(chatModel).build();
  }

  @Bean
  public ResolutionAssistant resolutionAssistant(ChatModel chatModel) {
    return AiServices.builder(ResolutionAssistant.class).chatModel(chatModel).build();
  }

  @Bean
  public BillingAssistant billingAssistant(ChatModel chatModel) {
    return AiServices.builder(BillingAssistant.class).chatModel(chatModel).build();
  }

  @Bean
  public RiskAssistant riskAssistant(ChatModel chatModel) {
    return AiServices.builder(RiskAssistant.class).chatModel(chatModel).build();
  }

  @Bean
  public CommunicatorAssistant communicatorAssistant(ChatModel chatModel) {
    return AiServices.builder(CommunicatorAssistant.class).chatModel(chatModel).build();
  }
}
