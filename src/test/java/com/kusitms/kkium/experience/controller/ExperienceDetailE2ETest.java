package com.kusitms.kkium.experience.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kusitms.kkium.auth.utils.JwtTokenProvider;
import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.ExperienceOrder;
import com.kusitms.kkium.experience.domain.Piece;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.repository.ExperienceOrderRepository;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.experience.repository.PieceRepository;
import com.kusitms.kkium.global.IntegrationTestBase;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

@Transactional
class ExperienceDetailE2ETest extends IntegrationTestBase {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private UserRepository userRepository;
  @Autowired private PieceRepository pieceRepository;
  @Autowired private ExperienceRepository experienceRepository;
  @Autowired private ExperienceOrderRepository experienceOrderRepository;

  private MockMvc mockMvc;

  @BeforeEach
  void setUpMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @DisplayName("E2E: 경험 상세 조회 API가 인증 통과 후 200과 정상 데이터를 반환한다")
  void getDetail_E2E_정상_조회() throws Exception {
    // given — 선행 데이터 저장
    User user =
        userRepository.save(
            User.basicLoginBuilder()
                .name("E2E유저")
                .email("e2e@kkium.com")
                .password("password")
                .build());
    Piece piece = pieceRepository.save(Piece.builder().type(PieceType.ACTIVITY).user(user).build());
    Experience experience =
        experienceRepository.save(Experience.builder().title("E2E 테스트 경험").piece(piece).build());
    experienceOrderRepository.save(
        ExperienceOrder.builder()
            .sortOrder(1)
            .pieceType(PieceType.ALL)
            .experience(experience)
            .user(user)
            .build());

    String token = jwtTokenProvider.createAccessToken(user.getId().toString());

    // when & then
    mockMvc
        .perform(
            get("/api/v1/experiences/{id}", experience.getId())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("E2E 테스트 경험"));
  }

  @Test
  @DisplayName("E2E: Authorization 헤더 없이 경험 상세 조회 시 401을 반환한다")
  void getDetail_E2E_인증없음_401() throws Exception {
    mockMvc.perform(get("/api/v1/experiences/1")).andExpect(status().isUnauthorized());
  }
}
