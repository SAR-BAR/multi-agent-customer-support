package com.sarbar.support.adapter.out.agent;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.port.out.CommunicatorAgent;
import com.sarbar.support.domain.record.Decision;
import com.sarbar.support.domain.record.post.ReplyContent;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("scripted")
public class ScriptedCommunicatorAgent implements CommunicatorAgent {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public CasePost compose(List<CasePost> boardView, Decision decision) {
    Long caseId = boardView.getFirst().caseId();
    String message =
        switch (decision.type()) {
          case EXECUTE ->
              "Your issue has been resolved. The approved actions have been applied to your order. You should see the changes reflected shortly.";
          case ESCALATE ->
              "Your case has been escalated to a senior support specialist who will review it personally. You will hear from them within 24 hours.";
          case ASK ->
              "We need a bit more information to resolve your issue. Could you please provide additional details?";
          case DENY ->
              "After reviewing your case, we are unable to process this request. Please contact us if you have additional information.";
        };

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
