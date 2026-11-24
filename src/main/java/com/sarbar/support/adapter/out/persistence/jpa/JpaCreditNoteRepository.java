package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.CreditNoteEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCreditNoteRepository extends JpaRepository<CreditNoteEntity, Long> {

  Optional<CreditNoteEntity> findByCreditNoteNumber(String creditNoteNumber);
}
