package com.kusitms.kkium.jd.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kusitms.kkium.global.utils.LlmEmbeddingService;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.domain.type.AnalysisStatus;
import com.kusitms.kkium.jd.repository.JdEmbeddingRepository;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.LlmJdAnalyzer;
import com.kusitms.kkium.user.domain.User;

@ExtendWith(MockitoExtension.class)
class JdEmbeddingServiceTest {

  @Mock private LlmJdAnalyzer llmJdAnalyzer;

  @Mock private LlmEmbeddingService llmEmbeddingService;

  @Mock private JdRepository jdRepository;

  @Mock private JdQuestionRepository jdQuestionRepository;

  @Mock private JdEmbeddingRepository jdEmbeddingRepository;

  private JdEmbeddingService jdEmbeddingService;

  @BeforeEach
  void setUp() {
    jdEmbeddingService =
        new JdEmbeddingService(
            llmJdAnalyzer,
            llmEmbeddingService,
            jdRepository,
            jdQuestionRepository,
            jdEmbeddingRepository);
  }

  @Test
  @DisplayName("공고 분석 결과와 문항으로 임베딩을 생성하고 저장한다")
  void analyzeAndSaveJdAndQuestionEmbeddings() {
    Long jdId = 1L;
    Long questionId = 10L;
    String content = "공고 본문";
    float[] jdEmbedding = new float[] {0.1f, 0.2f, 0.3f};
    float[] questionEmbedding = new float[] {0.4f, 0.5f, 0.6f};
    Jd jd = createJd();
    setId(jd, jdId);
    JdQuestion question = createQuestion(jd, questionId);
    LlmJdAnalyzer.AnalyzedJd analyzed =
        new LlmJdAnalyzer.AnalyzedJd("Java, Spring", "협업, 문제 해결", "주요 업무", "필수 자격", "우대 사항");

    when(jdRepository.findById(jdId)).thenReturn(Optional.of(jd));
    when(llmJdAnalyzer.analyze(content)).thenReturn(analyzed);
    when(llmEmbeddingService.embed("Java, Spring\n협업, 문제 해결\n주요 업무\n필수 자격\n우대 사항"))
        .thenReturn(jdEmbedding);
    when(jdQuestionRepository.findByJdOrderByOrderNum(jd)).thenReturn(List.of(question));
    when(llmEmbeddingService.embed(question.getContent())).thenReturn(questionEmbedding);

    jdEmbeddingService.analyzeAndUpdate(jdId, content);

    assertThat(jd.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
    assertThat(jd.getMainResponsibilities()).isEqualTo("주요 업무");
    assertThat(jd.getRequiredQualifications()).isEqualTo("필수 자격");
    assertThat(jd.getPreferredQualifications()).isEqualTo("우대 사항");
    assertThat(jd.getHardSkill()).isEqualTo("Java, Spring");
    assertThat(jd.getSoftSkill()).isEqualTo("협업, 문제 해결");
    verify(jdEmbeddingRepository).saveEmbedding(jdId, jdEmbedding);
    verify(jdEmbeddingRepository).saveQuestionEmbedding(questionId, questionEmbedding);
  }

  @Test
  @DisplayName("공고 임베딩 결과가 null이면 공고 임베딩을 저장하지 않는다")
  void skipJdEmbeddingWhenEmbeddingResultIsNull() {
    Long jdId = 1L;
    Long questionId = 10L;
    String content = "공고 본문";
    float[] questionEmbedding = new float[] {0.4f, 0.5f, 0.6f};
    Jd jd = createJd();
    setId(jd, jdId);
    JdQuestion question = createQuestion(jd, questionId);
    LlmJdAnalyzer.AnalyzedJd analyzed =
        new LlmJdAnalyzer.AnalyzedJd("Java", null, "주요 업무", null, null);

    when(jdRepository.findById(jdId)).thenReturn(Optional.of(jd));
    when(llmJdAnalyzer.analyze(content)).thenReturn(analyzed);
    when(llmEmbeddingService.embed("Java\n주요 업무")).thenReturn(null);
    when(jdQuestionRepository.findByJdOrderByOrderNum(jd)).thenReturn(List.of(question));
    when(llmEmbeddingService.embed(question.getContent())).thenReturn(questionEmbedding);

    jdEmbeddingService.analyzeAndUpdate(jdId, content);

    assertThat(jd.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
    verify(jdEmbeddingRepository, never()).saveEmbedding(any(), any());
    verify(jdEmbeddingRepository).saveQuestionEmbedding(questionId, questionEmbedding);
  }

  @Test
  @DisplayName("공고 분석 결과가 모두 비어 있으면 공고 임베딩을 요청하지 않는다")
  void skipJdEmbeddingWhenAnalyzedTextIsBlank() {
    Long jdId = 1L;
    String content = "공고 본문";
    Jd jd = createJd();
    setId(jd, jdId);
    LlmJdAnalyzer.AnalyzedJd analyzed = new LlmJdAnalyzer.AnalyzedJd(" ", null, "", " ", null);

    when(jdRepository.findById(jdId)).thenReturn(Optional.of(jd));
    when(llmJdAnalyzer.analyze(content)).thenReturn(analyzed);
    when(jdQuestionRepository.findByJdOrderByOrderNum(jd)).thenReturn(List.of());

    jdEmbeddingService.analyzeAndUpdate(jdId, content);

    assertThat(jd.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
    verify(llmEmbeddingService, never()).embed(any());
    verify(jdEmbeddingRepository, never()).saveEmbedding(any(), any());
  }

  @Test
  @DisplayName("공고 임베딩 텍스트는 null과 blank를 제외하고 주요 필드를 줄바꿈으로 연결한다")
  void buildEmbeddingTextWithNonBlankFields() {
    Long jdId = 1L;
    String content = "공고 본문";
    Jd jd = createJd();
    setId(jd, jdId);
    LlmJdAnalyzer.AnalyzedJd analyzed =
        new LlmJdAnalyzer.AnalyzedJd("Java", null, "주요 업무", " ", "우대 사항");

    when(jdRepository.findById(jdId)).thenReturn(Optional.of(jd));
    when(llmJdAnalyzer.analyze(content)).thenReturn(analyzed);
    when(jdQuestionRepository.findByJdOrderByOrderNum(jd)).thenReturn(List.of());

    jdEmbeddingService.analyzeAndUpdate(jdId, content);

    ArgumentCaptor<String> embeddingTextCaptor = ArgumentCaptor.forClass(String.class);
    verify(llmEmbeddingService).embed(embeddingTextCaptor.capture());
    assertThat(embeddingTextCaptor.getValue()).isEqualTo("Java\n주요 업무\n우대 사항");
  }

  private Jd createJd() {
    return Jd.builder()
        .user(User.basicLoginBuilder().name("유저").email("user@example.com").password("pw").build())
        .postingTitle("백엔드 개발자")
        .companyName("끼움")
        .recruitmentField("백엔드")
        .rawText("공고 본문")
        .build();
  }

  private JdQuestion createQuestion(Jd jd, Long questionId) {
    JdQuestion question = JdQuestion.builder().jd(jd).orderNum(1).content("지원 동기를 작성해주세요.").build();
    setId(question, questionId);
    return question;
  }

  private void setId(Object target, Long id) {
    try {
      Field field = target.getClass().getDeclaredField("id");
      field.setAccessible(true);
      field.set(target, id);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to set test id", e);
    }
  }
}
