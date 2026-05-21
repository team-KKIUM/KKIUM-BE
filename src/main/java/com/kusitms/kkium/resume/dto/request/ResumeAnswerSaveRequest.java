package com.kusitms.kkium.resume.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResumeAnswerSaveRequest(@NotNull @Valid List<AnswerRequest> answers) {

  public record AnswerRequest(
      @NotNull Long jdQuestionId,
      @Size(max = 2000) String answerText,
      @Size(max = 3) List<Long> experienceIds) {}
}
