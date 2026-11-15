package com.sarbar.support.adapter.out.agent.llm;

public class AgentFailureException extends RuntimeException {

  public AgentFailureException(String message, Throwable cause) {
    super(message, cause);
  }
}
