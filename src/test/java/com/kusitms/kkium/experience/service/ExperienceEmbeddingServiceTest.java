package com.kusitms.kkium.experience.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kusitms.kkium.experience.domain.type.TagCategory;
import com.kusitms.kkium.experience.dto.request.TagCreateRequest;
import com.kusitms.kkium.experience.repository.PieceEmbeddingRepository;
import com.kusitms.kkium.global.utils.LlmEmbeddingService;

@ExtendWith(MockitoExtension.class)
class ExperienceEmbeddingServiceTest {

  @Mock private LlmEmbeddingService llmEmbeddingService;

  @Mock private PieceEmbeddingRepository pieceEmbeddingRepository;

  private ExperienceEmbeddingService experienceEmbeddingService;

  @BeforeEach
  void setUp() {
    experienceEmbeddingService =
        new ExperienceEmbeddingService(llmEmbeddingService, pieceEmbeddingRepository);
  }

  @Test
  @DisplayName("경험 필드와 태그로 임베딩 텍스트를 구성하고 Piece 임베딩을 저장한다")
  void buildEmbeddingTextAndSavePieceEmbedding() {
    Long pieceId = 1L;
    float[] embedding = new float[] {0.1f, 0.2f, 0.3f};
    List<TagCreateRequest> tags =
        List.of(
            new TagCreateRequest(TagCategory.TECH, "Java"),
            new TagCreateRequest(TagCategory.COMPETENCY, "문제해결"));

    when(llmEmbeddingService.embed(any(String.class))).thenReturn(embedding);

    experienceEmbeddingService.embedPiece(
        pieceId,
        "정산 배치 안정화",
        "장애율을 낮춘 프로젝트",
        "배치가 자주 실패했습니다.",
        "안정화가 필요했습니다.",
        "재시도 로직을 개선했습니다.",
        "실패율을 낮췄습니다.",
        "모니터링의 중요성을 배웠습니다.",
        "동아리",
        "백엔드",
        "끼움",
        "인턴",
        "교육기관",
        tags);

    ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
    verify(llmEmbeddingService).embed(textCaptor.capture());
    assertThat(textCaptor.getValue())
        .isEqualTo(
            """
            정산 배치 안정화
            장애율을 낮춘 프로젝트
            배치가 자주 실패했습니다.
            안정화가 필요했습니다.
            재시도 로직을 개선했습니다.
            실패율을 낮췄습니다.
            모니터링의 중요성을 배웠습니다.
            동아리
            백엔드
            끼움
            인턴
            교육기관
            Java
            문제해결"""
                .stripIndent());
    verify(pieceEmbeddingRepository).saveEmbedding(pieceId, embedding);
  }

  @Test
  @DisplayName("null과 blank 필드는 임베딩 텍스트에서 제외한다")
  void excludeNullAndBlankFieldsFromEmbeddingText() {
    Long pieceId = 1L;
    float[] embedding = new float[] {0.1f, 0.2f, 0.3f};
    List<TagCreateRequest> tags =
        List.of(
            new TagCreateRequest(TagCategory.TECH, "Spring"),
            new TagCreateRequest(TagCategory.COMPETENCY, " "));

    when(llmEmbeddingService.embed(any(String.class))).thenReturn(embedding);

    experienceEmbeddingService.embedPiece(
        pieceId, "프로젝트 제목", " ", null, "담당 과제", "", "성과", null, null, "팀 리더", null, null, "교육기관",
        tags);

    ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
    verify(llmEmbeddingService).embed(textCaptor.capture());
    assertThat(textCaptor.getValue()).isEqualTo("프로젝트 제목\n담당 과제\n성과\n팀 리더\n교육기관\nSpring");
    verify(pieceEmbeddingRepository).saveEmbedding(pieceId, embedding);
  }

  @Test
  @DisplayName("OpenAI 임베딩 결과가 null이면 Piece 임베딩을 저장하지 않는다")
  void skipSaveWhenEmbeddingResultIsNull() {
    Long pieceId = 1L;

    when(llmEmbeddingService.embed("프로젝트 제목")).thenReturn(null);

    experienceEmbeddingService.embedPiece(
        pieceId, "프로젝트 제목", null, null, null, null, null, null, null, null, null, null, null, null);

    verify(pieceEmbeddingRepository, never()).saveEmbedding(any(), any());
  }

  @Test
  @DisplayName("임베딩할 텍스트가 없으면 OpenAI 임베딩을 요청하지 않는다")
  void skipEmbeddingWhenTextIsBlank() {
    Long pieceId = 1L;
    List<TagCreateRequest> tags = List.of(new TagCreateRequest(TagCategory.TECH, " "));

    experienceEmbeddingService.embedPiece(
        pieceId, " ", null, "", null, null, null, null, null, null, null, null, null, tags);

    verify(llmEmbeddingService, never()).embed(any());
    verify(pieceEmbeddingRepository, never()).saveEmbedding(any(), any());
  }
}
