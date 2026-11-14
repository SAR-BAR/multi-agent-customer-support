package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.CasePost;
import java.util.List;

public interface RiskAgent {

  CasePost evaluate(List<CasePost> boardView, CasePost proposal);
}
