package com.sarbar.support.adapter.out.agent.llm;

import com.sarbar.support.domain.model.CasePost;
import java.util.List;

final class BoardFormatter {

  private BoardFormatter() {}

  static String format(List<CasePost> posts) {
    var sb = new StringBuilder();
    for (CasePost post : posts) {
      sb.append("[Post #").append(post.id()).append("] ");
      sb.append("type=").append(post.type()).append(" ");
      sb.append("author=").append(post.author()).append(" ");
      if (post.replyTo() != null) {
        sb.append("replyTo=#").append(post.replyTo()).append(" ");
      }
      sb.append("\n").append(post.contentJson()).append("\n\n");
    }
    return sb.toString();
  }
}
