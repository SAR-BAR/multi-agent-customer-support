package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostType;
import java.util.List;
import java.util.Optional;

public interface CasePostRepository {

  List<CasePost> findByCaseId(Long caseId);

  List<CasePost> findByCaseIdAndType(Long caseId, PostType type);

  Optional<CasePost> findById(Long id);

  CasePost append(CasePost post);
}
