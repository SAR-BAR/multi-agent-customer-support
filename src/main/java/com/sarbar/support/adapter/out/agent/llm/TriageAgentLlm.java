package com.sarbar.support.adapter.out.agent.llm;

import com.sarbar.support.domain.port.out.TriageAgent;
import com.sarbar.support.domain.record.TriageFinding;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class TriageAgentLlm implements TriageAgent {

  private static final Logger log = LoggerFactory.getLogger(TriageAgentLlm.class);

  private final TriageAssistant assistant;

  public TriageAgentLlm(TriageAssistant assistant) {
    this.assistant = assistant;
  }

  @Override
  public TriageFinding analyze(List<String> conversationHistory, String currentMessage) {
    String prompt = buildPrompt(conversationHistory, currentMessage);
    try {
      TriageFinding finding = assistant.analyze(prompt);
      log.info(
          "Triage finding: problems={}, generalType={}",
          finding.problems().size(),
          finding.generalType());
      return validate(finding);
    } catch (Exception e) {
      log.warn("Triage failed, retrying once: {}", e.getMessage());
      try {
        TriageFinding finding = assistant.analyze(prompt);
        return validate(finding);
      } catch (Exception retryEx) {
        log.error("Triage retry failed, escalating: {}", retryEx.getMessage());
        throw new AgentFailureException("Triage agent failed after retry", retryEx);
      }
    }
  }

  private String buildPrompt(List<String> history, String currentMessage) {
    var sb = new StringBuilder();
    if (!history.isEmpty()) {
      sb.append("Previous messages:\n");
      for (String msg : history) {
        sb.append("- ").append(msg).append("\n");
      }
      sb.append("\n");
    }
    sb.append("Current message:\n").append(currentMessage);
    return sb.toString();
  }

  private TriageFinding validate(TriageFinding finding) {
    if (finding.problems() == null
        || finding.orderIds() == null
        || finding.missingFields() == null) {
      throw new IllegalStateException("Triage finding has null required fields");
    }
    return finding;
  }
}
