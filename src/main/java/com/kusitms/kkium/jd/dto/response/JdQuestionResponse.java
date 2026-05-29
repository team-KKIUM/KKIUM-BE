package com.kusitms.kkium.jd.dto.response;

import com.kusitms.kkium.jd.domain.JdAnswer;
import com.kusitms.kkium.jd.domain.JdQuestion;

public record JdQuestionResponse(
    Long questionId,
    int orderNum,
    String content,
    String answer,
    String aiDraft,
    boolean hasAiDraft) {

  public static JdQuestionResponse from(JdQuestion question, JdAnswer answer) {
    String aiDraft = answer != null ? answer.getAiDraft() : null;

    return new JdQuestionResponse(
        question.getId(),
        question.getOrderNum(),
        question.getContent(),
        answer != null ? answer.getContent() : null,
        aiDraft,
        aiDraft != null && !aiDraft.isBlank());
  }
}
