package com.sarbar.support.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sarbar.support.adapter.out.persistence.entity.ChatMemoryMessageEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaChatMemoryRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatMemoryIsolationTest {

  private final Map<Long, List<ChatMemoryMessageEntity>> rows = new ConcurrentHashMap<>();
  private JpaChatMemoryStore store;

  @BeforeEach
  void setUp() {
    rows.clear();
    JpaChatMemoryRepository repo = mock(JpaChatMemoryRepository.class);

    when(repo.findByConversationIdOrderByCreatedAtAsc(any()))
        .thenAnswer(inv -> new ArrayList<>(rows.getOrDefault(inv.getArgument(0), List.of())));

    doAnswer(
            inv -> {
              rows.remove((Long) inv.getArgument(0));
              return null;
            })
        .when(repo)
        .deleteByConversationId(any());

    when(repo.save(any()))
        .thenAnswer(
            inv -> {
              ChatMemoryMessageEntity e = inv.getArgument(0);
              rows.computeIfAbsent(e.getConversationId(), k -> new ArrayList<>()).add(e);
              return e;
            });

    store = new JpaChatMemoryStore(repo);
  }

  @Test
  void twoConversations_neverShareMemory() {
    Long conv1 = 1001L;
    Long conv2 = 1002L;

    var memory1 =
        MessageWindowChatMemory.builder().id(conv1).maxMessages(20).chatMemoryStore(store).build();
    var memory2 =
        MessageWindowChatMemory.builder().id(conv2).maxMessages(20).chatMemoryStore(store).build();

    memory1.add(UserMessage.from("I want a refund for ORD-001"));
    memory1.add(AiMessage.from("Looking into ORD-001"));

    memory2.add(UserMessage.from("Where is my order ORD-002?"));
    memory2.add(AiMessage.from("Checking ORD-002 status"));

    List<ChatMessage> messages1 = store.getMessages(conv1);
    List<ChatMessage> messages2 = store.getMessages(conv2);

    assertThat(messages1).hasSize(2);
    assertThat(messages2).hasSize(2);

    assertThat(messages1)
        .noneMatch(m -> m instanceof UserMessage um && um.singleText().contains("ORD-002"));
    assertThat(messages2)
        .noneMatch(m -> m instanceof UserMessage um && um.singleText().contains("ORD-001"));

    assertThat(messages1)
        .anyMatch(m -> m instanceof UserMessage um && um.singleText().contains("ORD-001"));
    assertThat(messages2)
        .anyMatch(m -> m instanceof UserMessage um && um.singleText().contains("ORD-002"));
  }

  @Test
  void deleteMessages_onlyAffectsTargetConversation() {
    Long conv3 = 1003L;
    Long conv4 = 1004L;

    store.updateMessages(conv3, List.of(UserMessage.from("msg for conv3")));
    store.updateMessages(conv4, List.of(UserMessage.from("msg for conv4")));

    store.deleteMessages(conv3);

    assertThat(store.getMessages(conv3)).isEmpty();
    assertThat(store.getMessages(conv4)).hasSize(1);
  }
}
