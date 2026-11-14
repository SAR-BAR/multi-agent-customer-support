package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.CasePostEntity;
import com.sarbar.support.domain.model.PostType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCasePostRepository extends JpaRepository<CasePostEntity, Long> {

  List<CasePostEntity> findByCaseIdOrderByCreatedAtAsc(Long caseId);

  List<CasePostEntity> findByCaseIdAndTypeOrderByCreatedAtAsc(Long caseId, PostType type);
}
