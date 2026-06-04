package com.kusitms.kkium.jd.utils.llm;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.utils.llm.result.LlmExperienceDetailResult;
import com.kusitms.kkium.jd.utils.llm.result.LlmMatchResult;
import com.kusitms.kkium.jd.utils.llm.result.LlmQuestionMatchResult;
import com.kusitms.kkium.jd.utils.llm.result.LlmWritingGuideResult;

import lombok.RequiredArgsConstructor;

/**
 * LLM 기반 경험-공고 적합도 분석 서비스 (오케스트레이션)
 *
 * <p>프롬프트 빌드 → API 호출 → 파싱의 흐름을 조율한다. - 프롬프트 빌드: {@link JdMatchPromptBuilder} - API 호출: {@link
 * LlmApiClient} - 응답 파싱: {@link LlmMatchResultParser}
 */
@Component
@RequiredArgsConstructor
public class LlmMatchScoreService {

  private final JdMatchPromptBuilder promptBuilder;
  private final LlmApiClient apiClient;
  private final LlmMatchResultParser parser;

  /** LLM 1번 호출로 활용 적합도(경험별) + 지원 적합도(포트폴리오 종합)를 한꺼번에 반환 */
  public LlmMatchResult scoreAll(Jd jd, List<Experience> experiences) {
    if (experiences.isEmpty()) return new LlmMatchResult(Map.of(), 0);
    String response =
        apiClient.call(
            promptBuilder.buildCombinedPrompt(jd, experiences),
            promptBuilder.buildCombinedSchema());
    return parser.parseCombinedResult(response, experiences);
  }

  /** 문항 컨텍스트를 포함해 활용 적합도를 계산 (자소서 작성 화면 경험 선택 모달용) */
  public LlmQuestionMatchResult scoreAllByQuestion(
      Jd jd, JdQuestion question, List<Experience> experiences) {
    if (experiences.isEmpty()) return new LlmQuestionMatchResult(Map.of());
    String response =
        apiClient.call(
            promptBuilder.buildQuestionCombinedPrompt(jd, question, experiences),
            promptBuilder.buildQuestionCombinedSchema());
    return parser.parseQuestionMatchResult(response, experiences);
  }

  /** 선택된 경험들을 기반으로 자소서 작성 가이드 생성 */
  public LlmWritingGuideResult generateWritingGuide(
      Jd jd, JdQuestion question, List<Experience> experiences) {
    if (experiences.isEmpty()) return LlmWritingGuideResult.empty();
    String response =
        apiClient.call(
            promptBuilder.buildWritingGuidePrompt(jd, question, experiences),
            promptBuilder.buildWritingGuideSchema());
    return parser.parseWritingGuideResult(response);
  }

  /** 경험 카드 클릭 시 상세 분석 */
  public LlmExperienceDetailResult analyzeExperienceDetail(Jd jd, Experience experience) {
    String response =
        apiClient.call(
            promptBuilder.buildExperienceDetailPrompt(jd, experience),
            promptBuilder.buildExperienceDetailSchema());
    return parser.parseExperienceDetailResult(response);
  }
}
