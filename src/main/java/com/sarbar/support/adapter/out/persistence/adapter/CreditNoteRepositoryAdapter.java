package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.CreditNoteEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaCreditNoteRepository;
import com.sarbar.support.domain.model.CreditNote;
import com.sarbar.support.domain.port.out.CreditNoteRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CreditNoteRepositoryAdapter implements CreditNoteRepository {

  private final JpaCreditNoteRepository jpa;

  public CreditNoteRepositoryAdapter(JpaCreditNoteRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public CreditNote save(CreditNote creditNote) {
    return jpa.save(CreditNoteEntity.fromDomain(creditNote)).toDomain();
  }

  @Override
  public Optional<CreditNote> findByCreditNoteNumber(String creditNoteNumber) {
    return jpa.findByCreditNoteNumber(creditNoteNumber).map(CreditNoteEntity::toDomain);
  }

  @Override
  public void deleteByCreditNoteNumber(String creditNoteNumber) {
    jpa.findByCreditNoteNumber(creditNoteNumber).ifPresent(jpa::delete);
  }
}
