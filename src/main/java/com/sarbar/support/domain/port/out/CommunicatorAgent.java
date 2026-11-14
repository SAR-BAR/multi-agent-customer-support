package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.record.Decision;
import java.util.List;

public interface CommunicatorAgent {

  CasePost compose(List<CasePost> boardView, Decision decision);
}
