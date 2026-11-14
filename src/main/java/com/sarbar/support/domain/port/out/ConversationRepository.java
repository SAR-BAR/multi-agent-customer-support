package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.Conversation;
import java.util.Optional;

public interface ConversationRepository {

  Optional<Conversation> findById(Long id);

  Conversation save(Conversation conversation);
}
