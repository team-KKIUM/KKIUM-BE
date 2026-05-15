package com.kusitms.kkium.experience.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.kusitms.kkium.experience.dto.request.TagCreateRequest;
import com.kusitms.kkium.experience.repository.PieceEmbeddingRepository;
import com.kusitms.kkium.global.utils.LlmEmbeddingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceEmbeddingService {

  private final LlmEmbeddingService llmEmbeddingService;
  private final PieceEmbeddingRepository pieceEmbeddingRepository;

  @Async("jdAnalysisExecutor")
  public void embedPiece(
      Long pieceId,
      String title,
      String oneLineIntro,
      String situation,
      String task,
      String act,
      String result,
      String taken,
      String name,
      String role,
      String company,
      String employmentStatus,
      String organizationName,
      List<TagCreateRequest> tags) {
    try {
      String text =
          buildEmbeddingText(
              title,
              oneLineIntro,
              situation,
              task,
              act,
              result,
              taken,
              name,
              role,
              company,
              employmentStatus,
              organizationName,
              tags);
      if (text == null) return;

      float[] embedding = llmEmbeddingService.embed(text);
      if (embedding != null) {
        pieceEmbeddingRepository.saveEmbedding(pieceId, embedding);
        log.info("Piece 임베딩 완료 - pieceId: {}", pieceId);
      }
    } catch (Exception e) {
      log.warn("Piece 임베딩 실패 - pieceId: {}, 원인: {}", pieceId, e.getMessage());
    }
  }

  private String buildEmbeddingText(
      String title,
      String oneLineIntro,
      String situation,
      String task,
      String act,
      String result,
      String taken,
      String name,
      String role,
      String company,
      String employmentStatus,
      String organizationName,
      List<TagCreateRequest> tags) {
    String tagText =
        tags == null
            ? null
            : tags.stream()
                .map(TagCreateRequest::field)
                .filter(f -> f != null && !f.isBlank())
                .collect(Collectors.joining("\n"));

    String text =
        Stream.of(
                title,
                oneLineIntro,
                situation,
                task,
                act,
                result,
                taken,
                name,
                role,
                company,
                employmentStatus,
                organizationName,
                tagText)
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining("\n"));
    return text.isBlank() ? null : text;
  }
}
