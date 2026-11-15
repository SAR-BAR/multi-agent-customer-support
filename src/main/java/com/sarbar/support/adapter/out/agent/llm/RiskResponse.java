package com.sarbar.support.adapter.out.agent.llm;

import java.util.List;

public record RiskResponse(
    String type, String verdict, String question, List<String> signalsCited) {}
