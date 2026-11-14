package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.CasePost;
import java.util.List;

public interface ResolutionAgent {

  List<CasePost> investigate(List<CasePost> boardView);
}
