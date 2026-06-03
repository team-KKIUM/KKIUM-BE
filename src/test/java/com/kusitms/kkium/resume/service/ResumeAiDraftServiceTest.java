package com.kusitms.kkium.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.Piece;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdAnswer;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.repository.JdAnswerRepository;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.resume.dto.response.AiDraftResponse;
import com.kusitms.kkium.resume.utils.llm.GeminiAiDraftService;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ResumeAiDraftServiceTest {

  @Mock private JdRepository jdRepository;

  @Mock private JdQuestionRepository jdQuestionRepository;

  @Mock private JdAnswerRepository jdAnswerRepository;

  @Mock private ExperienceRepository experienceRepository;

  @Mock private UserRepository userRepository;

  @Mock private GeminiAiDraftService geminiAiDraftService;

  private ResumeAiDraftService resumeAiDraftService;

  @BeforeEach
  void setUp() {
    resumeAiDraftService =
        new ResumeAiDraftService(
            jdRepository,
            jdQuestionRepository,
            jdAnswerRepository,
            experienceRepository,
            userRepository,
            geminiAiDraftService);
  }

  @Test
  @DisplayName("정상 요청이면 Gemini로 AI 초안을 생성하고 JdAnswer에 저장한다")
  void generateAiDraftAndSave() {
    Long userId = 1L;
    Long jdId = 10L;
    Long questionId = 20L;
    Long experienceId = 30L;
    String draft = "토스에서 정산 배치 안정화 프로젝트를 진행하며...";

    User user = createUser(userId);
    Jd jd = createJd(user);
    setId(jd, jdId);
    JdQuestion question = createQuestion(jd);
    setId(question, questionId);
    Experience experience = createExperience(user);
    setId(experience, experienceId);

    when(jdRepository.findById(jdId)).thenReturn(Optional.of(jd));
    when(jdQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
    when(experienceRepository.findAllByIdIn(List.of(experienceId))).thenReturn(List.of(experience));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(jdAnswerRepository.findByJdQuestionAndUser(question, user)).thenReturn(Optional.empty());
    when(geminiAiDraftService.generateAiDraft(jd, question, List.of(experience))).thenReturn(draft);
    when(jdAnswerRepository.save(any(JdAnswer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AiDraftResponse response =
        resumeAiDraftService.generateAiDraft(jdId, questionId, List.of(experienceId), userId);

    assertThat(response.draft()).isEqualTo(draft);

    ArgumentCaptor<JdAnswer> answerCaptor = ArgumentCaptor.forClass(JdAnswer.class);
    verify(jdAnswerRepository).save(answerCaptor.capture());
    JdAnswer savedAnswer = answerCaptor.getValue();
    assertThat(savedAnswer.getJdQuestion()).isEqualTo(question);
    assertThat(savedAnswer.getUser()).isEqualTo(user);
    assertThat(savedAnswer.getContent()).isEmpty();
    assertThat(savedAnswer.getAiDraft()).isEqualTo(draft);
    verify(geminiAiDraftService).generateAiDraft(jd, question, List.of(experience));
  }

  private User createUser(Long id) {
    User user =
        User.basicLoginBuilder()
            .name("테스트 유저")
            .email("test@example.com")
            .password("encoded-password")
            .build();
    setId(user, id);
    return user;
  }

  private Jd createJd(User user) {
    return Jd.builder()
        .user(user)
        .postingTitle("백엔드 개발자")
        .companyName("끼움")
        .recruitmentField("백엔드")
        .rawText("공고 본문")
        .build();
  }

  private JdQuestion createQuestion(Jd jd) {
    return JdQuestion.builder().jd(jd).orderNum(1).content("지원 동기를 작성해주세요.").build();
  }

  private Experience createExperience(User user) {
    Piece piece = Piece.builder().type(PieceType.ACTIVITY).user(user).build();
    return Experience.builder()
        .piece(piece)
        .title("정산 배치 안정화")
        .oneLineIntro("장애율을 낮춘 프로젝트")
        .situation("배치가 자주 실패했습니다.")
        .task("안정화가 필요했습니다.")
        .act("재시도 로직을 개선했습니다.")
        .result("실패율을 낮췄습니다.")
        .taken("모니터링의 중요성을 배웠습니다.")
        .build();
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
