package com.kusitms.kkium.experience.service;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
      String situation,
      String task,
      String act,
      String result,
      String taken) {
    try {
      String text = buildEmbeddingText(title, situation, task, act, result, taken);
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
      String title, String situation, String task, String act, String result, String taken) {
    String text =
        Stream.of(title, situation, task, act, result, taken)
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining("\n"));
    return text.isBlank() ? null : text;
  }
}
