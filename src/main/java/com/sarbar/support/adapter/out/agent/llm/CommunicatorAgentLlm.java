package com.sarbar.support.adapter.out.agent.llm;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.port.out.CommunicatorAgent;
import com.sarbar.support.domain.record.Decision;
import com.sarbar.support.domain.record.post.ReplyContent;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("local")
public class CommunicatorAgentLlm implements CommunicatorAgent {

  private static final Logger log = LoggerFactory.getLogger(CommunicatorAgentLlm.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final CommunicatorAssistant assistant;

  public CommunicatorAgentLlm(CommunicatorAssistant assistant) {
    this.assistant = assistant;
  }

  @Override
  public CasePost compose(List<CasePost> boardView, Decision decision) {
    Long caseId = boardView.getFirst().caseId();
    String context = buildContext(boardView, decision);
    try {
      CommunicatorResponse response = assistant.compose(context);
      return toPost(response, caseId);
    } catch (Exception e) {
      log.warn("Communicator agent failed, retrying: {}", e.getMessage());
      try {
        CommunicatorResponse response = assistant.compose(context);
        return toPost(response, caseId);
      } catch (Exception retryEx) {
        log.error("Communicator agent retry failed: {}", retryEx.getMessage());
        throw new AgentFailureException("Communicator agent failed after retry", retryEx);
      }
    }
  }

  private String buildContext(List<CasePost> boardView, Decision decision) {
    var sb = new StringBuilder();
    sb.append("=== Case Board ===\n");
    sb.append(BoardFormatter.format(boardView));
    sb.append("\n=== Decision ===\n");
    sb.append("Type: ").append(decision.type()).append("\n");
    if (decision.explanation() != null) {
      sb.append("Explanation: ").append(decision.explanation()).append("\n");
    }
    return sb.toString();
  }

  private CasePost toPost(CommunicatorResponse response, Long caseId) {
    String message =
        response.message() != null ? response.message() : "We are looking into your case.";
    ReplyContent content = new ReplyContent(message);
    return new CasePost(
        null,
        caseId,
        PostAuthor.COMMUNICATOR,
        PostType.REPLY,
        toJson(content),
        List.of(),
        null,
        Instant.now());
  }

  private String toJson(Object obj) {
    try {
      return MAPPER.writeValueAsString(obj);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize", e);
    }
  }
}
