package com.kusitms.kkium.jd.dto.response;

import com.kusitms.kkium.jd.domain.JdAnswer;
import com.kusitms.kkium.jd.domain.JdQuestion;

public record JdQuestionResponse(
    Long questionId, int orderNum, String content, String answer, boolean hasAiDraft) {

  public static JdQuestionResponse from(JdQuestion question, JdAnswer answer) {
    return new JdQuestionResponse(
        question.getId(),
        question.getOrderNum(),
        question.getContent(),
        answer != null ? answer.getContent() : null,
        answer != null && answer.getAiDraft() != null && !answer.getAiDraft().isBlank());
  }
}
