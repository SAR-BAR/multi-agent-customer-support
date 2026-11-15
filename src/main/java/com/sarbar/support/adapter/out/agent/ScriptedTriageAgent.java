package com.sarbar.support.adapter.out.agent;

import com.sarbar.support.domain.model.GeneralType;
import com.sarbar.support.domain.model.ProblemType;
import com.sarbar.support.domain.port.out.TriageAgent;
import com.sarbar.support.domain.record.ProblemRef;
import com.sarbar.support.domain.record.TriageFinding;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("scripted")
public class ScriptedTriageAgent implements TriageAgent {

  private static final Pattern ORDER_ID_PATTERN = Pattern.compile("ORD-\\d+");

  @Override
  public TriageFinding analyze(List<String> conversationHistory, String currentMessage) {
    String msg = currentMessage.toLowerCase();
    List<String> orderIds = extractOrderIds(currentMessage);
    List<ProblemRef> problems = new ArrayList<>();
    boolean legalThreat =
        msg.contains("lawyer")
            || msg.contains("legal")
            || msg.contains("court")
            || msg.contains("social media");
    boolean asksForHuman =
        msg.contains("human")
            || msg.contains("real person")
            || msg.contains("agent")
            || msg.contains("manager");
    double frustration = legalThreat ? 0.9 : (asksForHuman ? 0.7 : 0.3);

    GeneralType generalType = classifyGeneral(msg);
    if (generalType != GeneralType.NONE) {
      return new TriageFinding(
          List.of(),
          orderIds,
          generalType,
          frustration,
          legalThreat,
          asksForHuman,
          List.of(),
          false);
    }

    if (orderIds.isEmpty()) {
      return new TriageFinding(
          List.of(),
          List.of(),
          GeneralType.NONE,
          frustration,
          legalThreat,
          asksForHuman,
          List.of("orderId"),
          false);
    }

    String orderId = orderIds.getFirst();
    for (ProblemType pt : detectProblems(msg)) {
      problems.add(new ProblemRef(pt, orderId, pt.name().toLowerCase().replace('_', ' ')));
    }

    if (problems.isEmpty()) {
      problems.add(new ProblemRef(ProblemType.OTHER, orderId, "general order enquiry"));
    }

    return new TriageFinding(
        problems,
        orderIds,
        GeneralType.NONE,
        frustration,
        legalThreat,
        asksForHuman,
        List.of(),
        false);
  }

  private GeneralType classifyGeneral(String msg) {
    if (msg.contains("faq")
        || msg.contains("return policy")
        || msg.contains("how do i")
        || msg.contains("what is your")) {
      return GeneralType.FAQ;
    }
    if (msg.contains("change my email")
        || msg.contains("update my address")
        || msg.contains("account")) {
      return GeneralType.ACCOUNT_CHANGE;
    }
    if (msg.contains("feedback") || msg.contains("suggestion")) {
      return GeneralType.FEEDBACK;
    }
    return GeneralType.NONE;
  }

  private List<ProblemType> detectProblems(String msg) {
    List<ProblemType> types = new ArrayList<>();
    if (msg.contains("late")
        || msg.contains("delay")
        || msg.contains("not arrived")
        || msg.contains("where is my order")) {
      types.add(ProblemType.LATE_DELIVERY);
    }
    if (msg.contains("double charge")
        || msg.contains("charged twice")
        || msg.contains("duplicate payment")) {
      types.add(ProblemType.DOUBLE_CHARGE);
    }
    if (msg.contains("wrong fee")
        || msg.contains("delivery fee")
        || msg.contains("delivery charge")
        || msg.contains("shipping fee")) {
      types.add(ProblemType.WRONG_FEE);
    }
    if (msg.contains("wrong item") || msg.contains("wrong product")) {
      types.add(ProblemType.WRONG_ITEM);
    }
    if (msg.contains("not received")) {
      types.add(ProblemType.NOT_RECEIVED);
    }
    if (msg.contains("refund")) {
      types.add(ProblemType.REFUND_REQUEST);
    }
    if (msg.contains("failed") || msg.contains("debited but")) {
      types.add(ProblemType.DEBITED_ORDER_FAILED);
    }
    return types;
  }

  private List<String> extractOrderIds(String message) {
    List<String> ids = new ArrayList<>();
    Matcher m = ORDER_ID_PATTERN.matcher(message);
    while (m.find()) {
      ids.add(m.group());
    }
    return ids;
  }
}
