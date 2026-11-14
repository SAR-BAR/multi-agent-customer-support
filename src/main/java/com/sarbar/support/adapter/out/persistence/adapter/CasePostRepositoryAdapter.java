package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.CasePostEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaCasePostRepository;
import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.port.out.CasePostRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CasePostRepositoryAdapter implements CasePostRepository {

  private final JpaCasePostRepository jpa;

  public CasePostRepositoryAdapter(JpaCasePostRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public List<CasePost> findByCaseId(Long caseId) {
    return jpa.findByCaseIdOrderByCreatedAtAsc(caseId).stream()
        .map(CasePostEntity::toDomain)
        .toList();
  }

  @Override
  public List<CasePost> findByCaseIdAndType(Long caseId, PostType type) {
    return jpa.findByCaseIdAndTypeOrderByCreatedAtAsc(caseId, type).stream()
        .map(CasePostEntity::toDomain)
        .toList();
  }

  @Override
  public Optional<CasePost> findById(Long id) {
    return jpa.findById(id).map(CasePostEntity::toDomain);
  }

  @Override
  public CasePost append(CasePost post) {
    CasePostEntity saved = jpa.save(CasePostEntity.fromDomain(post));
    return saved.toDomain();
  }
}
