package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.ApprovedAnswer;
import com.sarbar.support.domain.model.GeneralType;
import java.util.Optional;

public interface ApprovedAnswerRepository {

  Optional<ApprovedAnswer> findByGeneralType(GeneralType generalType);

  Optional<ApprovedAnswer> findByGeneralTypeAndTopic(GeneralType generalType, String topic);
}
