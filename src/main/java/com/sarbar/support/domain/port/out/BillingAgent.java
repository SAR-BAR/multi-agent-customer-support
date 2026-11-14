package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.CasePost;
import java.util.List;

public interface BillingAgent {

  List<CasePost> investigate(List<CasePost> boardView);
}
