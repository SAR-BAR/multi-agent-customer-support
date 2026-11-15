package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.ChatMemoryMessageEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaChatMemoryRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaChatMemoryStore implements ChatMemoryStore {

  private final JpaChatMemoryRepository repository;

  public JpaChatMemoryStore(JpaChatMemoryRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ChatMessage> getMessages(Object memoryId) {
    Long conversationId = toConversationId(memoryId);
    List<ChatMemoryMessageEntity> entities =
        repository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    List<ChatMessage> messages = new ArrayList<>();
    for (ChatMemoryMessageEntity entity : entities) {
      ChatMessage msg = toMessage(entity.getRole(), entity.getContent());
      if (msg != null) messages.add(msg);
    }
    return messages;
  }

  @Override
  @Transactional
  public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    Long conversationId = toConversationId(memoryId);
    repository.deleteByConversationId(conversationId);
    for (ChatMessage message : messages) {
      String role = roleOf(message);
      String content = contentOf(message);
      repository.save(new ChatMemoryMessageEntity(conversationId, role, content));
    }
  }

  @Override
  @Transactional
  public void deleteMessages(Object memoryId) {
    Long conversationId = toConversationId(memoryId);
    repository.deleteByConversationId(conversationId);
  }

  private Long toConversationId(Object memoryId) {
    if (memoryId instanceof Long l) return l;
    return Long.parseLong(memoryId.toString());
  }

  private String roleOf(ChatMessage message) {
    return switch (message.type()) {
      case SYSTEM -> "SYSTEM";
      case USER -> "USER";
      case AI -> "AI";
      default -> "USER";
    };
  }

  private String contentOf(ChatMessage message) {
    return switch (message) {
      case SystemMessage sm -> sm.text();
      case UserMessage um -> um.singleText();
      case AiMessage am -> am.text();
      default -> message.toString();
    };
  }

  private ChatMessage toMessage(String role, String content) {
    return switch (role) {
      case "SYSTEM" -> SystemMessage.from(content);
      case "USER" -> UserMessage.from(content);
      case "AI" -> AiMessage.from(content);
      default -> UserMessage.from(content);
    };
  }
}
