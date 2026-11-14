package com.sarbar.support.domain.record.post;

import com.sarbar.support.domain.model.VerdictType;
import java.util.List;

public record VerdictContent(Long proposalPostId, VerdictType verdict, List<String> signalsCited) {}
