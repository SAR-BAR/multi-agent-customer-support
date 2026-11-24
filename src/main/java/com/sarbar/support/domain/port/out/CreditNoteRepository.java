package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.CreditNote;
import java.util.Optional;

public interface CreditNoteRepository {

  CreditNote save(CreditNote creditNote);

  Optional<CreditNote> findByCreditNoteNumber(String creditNoteNumber);

  void deleteByCreditNoteNumber(String creditNoteNumber);
}
