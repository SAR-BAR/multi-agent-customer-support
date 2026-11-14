package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.Case;
import java.util.List;
import java.util.Optional;

public interface CaseRepository {

  Optional<Case> findById(Long id);

  Optional<Case> findByConversationIdAndOrderId(Long conversationId, String orderId);

  List<Case> findByConversationId(Long conversationId);

  Case save(Case supportCase);
}
