package com.kusitms.kkium.jd.utils.llm;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;

@Component
public class JdMatchPromptBuilder {

  /** 활용적합도, 지원적합도 산출 프롬프트 */
  public String buildCombinedPrompt(Jd jd, List<Experience> experiences) {
    return """
        너는 채용 공고와 경험의 적합도를 평가하는 시스템이다.

        [규칙]
        - 반드시 제공된 내용만 근거로 사용하라.
        - 없는 내용을 추론하지 마라.
        - 점수는 0~100 사이 정수로 반환하라.
        - 반드시 JSON 형식으로만 응답하라.

        %s

        [경험 목록]
        %s

        [해야 할 일]
        1. 각 경험이 위 공고에 개별적으로 얼마나 활용 가능한지 평가하라. (usageScores)
        2. 위 경험들을 포트폴리오 전체 관점에서 공고 요구사항을 얼마나 커버하는지 평가하라. (applicationScore)

        반환 형식:
        {
          "usageScores": [
            { "pieceId": 숫자, "score": 숫자 },
            ...
          ],
          "applicationScore": 숫자
        }
        """
        .formatted(buildJdContext(jd), buildExperienceList(experiences));
  }

  /** 공고, 자소서문항, 지원자의 경험들 간의 적합도 산출 프롬프트 */
  public String buildQuestionCombinedPrompt(
      Jd jd, JdQuestion question, List<Experience> experiences) {
    return """
        너는 채용 공고와 자소서 문항, 그리고 지원자의 경험 간의 적합도를 평가하는 시스템이다.

        [규칙]
        - 반드시 제공된 내용만 근거로 사용하라.
        - 없는 내용을 추론하지 마라.
        - 점수는 0~100 사이 정수로 반환하라.
        - 반드시 JSON 형식으로만 응답하라.

        %s

        [자소서 문항]
        %s

        [경험 목록]
        %s

        [해야 할 일]
        각 경험이 위 공고와 자소서 문항에 얼마나 적합한지 개별적으로 평가하라.

        반환 형식:
        {
          "usageScores": [
            { "pieceId": 숫자, "score": 숫자 },
            ...
          ]
        }
        """
        .formatted(buildJdContext(jd), question.getContent(), buildExperienceList(experiences));
  }

  /** 하나의 경험이 공고와 연결되는 핵심키워드, 연결점, 작성가이드 반환하는 프롬프트 */
  public String buildWritingGuidePrompt(Jd jd, JdQuestion question, List<Experience> experiences) {
    return """
        너는 자기소개서 작성을 도와주는 전문 커리어 코치이다.

        [규칙]
        - 반드시 제공된 내용만 근거로 사용하라.
        - 없는 내용을 추론하거나 만들어내지 마라.
        - 반드시 JSON 형식으로만 응답하라.
        - 한국어로 작성하라.

        %s

        [자소서 문항]
        %s

        [선택된 경험 목록]
        %s

        [해야 할 일]
        1. coreKeywords: 공고의 주요 업무, 필수 역량, 우대 역량, 소프트 스킬에 실제로 존재하는 단어나 구문만 최대 5개 추출하라.
           위 공고 텍스트에 없는 단어는 절대 사용하지 마라. 10자 이내의 단어 또는 짧은 명사구로만 작성하라. 문장이나 설명 형태는 절대 사용하지 마라.
           단독으로는 의미가 불분명한 범용적인 단어는 반드시 앞 단어와 결합한 명사구로 작성하라.
        2. connectionToJd: 선택된 경험들을 종합적으로 보았을 때 공고의 어떤 요구사항들을 커버할 수 있는지 2문장 이내로 서술하라.
        3. writingGuide: 자소서를 작성하는 사람의 입장에서, 각 경험의 어떤 내용을 자소서에 녹이면 공고에 어필이 될지 조언하라.
           각 경험별로 경험 제목을 자연스럽게 문장에 녹여 "OOO 경험에서 ~을 언급하면 효과적입니다" 형태로 작성하라.
           해당 경험의 Situation/Task/Action/Result/Taken에 실제로 존재하는 내용만 근거로 사용하고, 수치가 있다면 반드시 언급하라.
           위 경험 데이터에 없는 수치, 사실, 성과를 절대 지어내거나 추론하지 마라.

        [말투 규칙]
        - 모든 문장은 반드시 '~합니다', '~입니다', '~습니다' 체로 통일하라.
        - '~하십시오', '~이다', '~한다', '~세요', '~어요' 등 다른 말투는 절대 사용하지 마라.

        반환 형식:
        {
          "coreKeywords": ["키워드1", "키워드2", ...],
          "connectionToJd": "공고와의 연결점 서술",
          "writingGuide": "구체적인 작성 팁"
        }
        """
        .formatted(buildJdContext(jd), question.getContent(), buildExperienceList(experiences));
  }

  /** 하나의 경험과 공고를 분석해서 경험의 강점, 보완할점, 작성가이드를 반환하는 프롬프트 */
  public String buildExperienceDetailPrompt(Jd jd, Experience experience) {
    return """
        너는 채용 공고와 지원자 경험을 비교 분석하는 전문가이다.

        [규칙]
        - 반드시 제공된 내용만 근거로 사용하라.
        - 없는 내용을 추론하거나 만들어내지 마라.
        - 반드시 JSON 형식으로만 응답하라.
        - 한국어로 작성하라.

        %s

        [경험 정보]
        - 제목: %s
        - 한줄소개: %s
        - Situation: %s
        - Task: %s
        - Action: %s
        - Result: %s
        - Taken: %s

        [해야 할 일]
        1. strengths: 이 경험이 공고 요구사항과 어떻게 직접적으로 연결되는지 2문장 이내로 서술하라.
        2. weaknesses: 이 경험에서 보완하면 공고에 더 잘 어필할 수 있는 점을 2문장 이내로 서술하라. 경험 자체의 아쉬운 점이 아니라 공고 관점에서 추가하면 좋을 내용을 제안하라.
        3. usageGuide: 이 경험을 자기소개서에서 어필할 때 어떤 포인트를 강조하면 좋을지 조언하는 방식으로 2문장 이내로 서술하라. "~을 강조하면 좋습니다", "~을 언급하면 효과적입니다" 처럼 조언하는 말투로 작성하라.
        4. highlightKeywords: 공고 텍스트에서 이 경험과 직접 연관되는 핵심 키워드를 최대 5개 추출하라.
           반드시 공고의 주요 업무, 필수 역량, 우대 역량, 기술 스택, 소프트 스킬에 실제로 존재하는 단어나 구문만 반환하라.
           각 키워드가 공고의 어느 섹션에서 나왔는지 sources에 함께 반환하라.
           sources는 다음 값 중에서만 선택하라: mainResponsibilities, requiredQualifications, preferredQualifications, hardSkill, softSkill
           키워드는 반드시 10자 이내의 단어 또는 짧은 명사구로만 작성하라.
           문장이나 설명 형태는 절대 사용하지 마라.
           여러 단어를 쉼표로 합쳐서 하나의 키워드로 만들지 마라.

        [말투 규칙]
        - 모든 문장은 반드시 '~합니다', '~입니다', '~습니다' 체로 통일하라.
        - '~하십시오', '~이다', '~한다', '~세요', '~어요' 등 다른 말투는 절대 사용하지 마라.

        반환 형식:
        {
          "strengths": "좋은 점 서술",
          "weaknesses": "보완하면 좋을 점 서술",
          "usageGuide": "자기소개서 어필 방법 서술",
          "highlightKeywords": [
            { "keyword": "키워드1", "sources": ["mainResponsibilities", "hardSkill"] },
            ...
          ]
        }
        """
        .formatted(
            buildJdContext(jd),
            experience.getTitle(),
            experience.getOneLineIntro(),
            experience.getSituation(),
            experience.getTask(),
            experience.getAct(),
            experience.getResult(),
            experience.getTaken());
  }

  private String buildJdContext(Jd jd) {
    return """
        [공고 정보]
        - 기업: %s
        - 직무: %s
        - 주요 업무: %s
        - 필수 역량: %s
        - 우대 역량: %s
        - 기술 스택: %s
        - 소프트 스킬: %s"""
        .formatted(
            jd.getCompanyName(),
            jd.getRecruitmentField(),
            jd.getMainResponsibilities(),
            jd.getRequiredQualifications(),
            jd.getPreferredQualifications(),
            jd.getHardSkill(),
            jd.getSoftSkill());
  }

  private String buildExperienceList(List<Experience> experiences) {
    return IntStream.range(0, experiences.size())
        .mapToObj(
            i -> {
              Experience e = experiences.get(i);
              return """
              경험 %d (pieceId: %d):
              - 제목: %s
              - 한줄소개: %s
              - Situation: %s
              - Task: %s
              - Action: %s
              - Result: %s
              - Taken: %s
              """
                  .formatted(
                      i + 1,
                      e.getPiece().getId(),
                      e.getTitle(),
                      e.getOneLineIntro(),
                      e.getSituation(),
                      e.getTask(),
                      e.getAct(),
                      e.getResult(),
                      e.getTaken());
            })
        .collect(Collectors.joining("\n"));
  }
}
