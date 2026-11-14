package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.record.TriageFinding;
import java.util.List;

public interface TriageAgent {

  TriageFinding analyze(List<String> conversationHistory, String currentMessage);
}
