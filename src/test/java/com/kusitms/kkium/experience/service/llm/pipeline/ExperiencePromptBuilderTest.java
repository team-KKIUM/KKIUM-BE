package com.kusitms.kkium.experience.service.llm.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

class ExperiencePromptBuilderTest {

  private ExperiencePromptBuilder promptBuilder;

  @BeforeEach
  void setUp() {
    promptBuilder = new ExperiencePromptBuilder();
  }

  private void setPromptTemplate(String template) throws IOException {
    Resource resource = new ByteArrayResource(template.getBytes(StandardCharsets.UTF_8));
    ReflectionTestUtils.setField(promptBuilder, "promptResource", resource);
    promptBuilder.init();
  }

  @Test
  @DisplayName("프롬프트 템플릿의 %s 자리에 입력 텍스트가 치환된다")
  void build_정상_치환() throws IOException {
    // given
    setPromptTemplate("경험을 분석하세요: %s\n결과를 JSON으로 반환");

    // when
    String result = promptBuilder.build("프로젝트 A 개발 경험");

    // then
    assertThat(result).contains("프로젝트 A 개발 경험").doesNotContain("%s");
  }

  @Test
  @DisplayName("빈 입력 텍스트도 정상적으로 치환된다")
  void build_빈_입력() throws IOException {
    // given
    setPromptTemplate("입력: [%s]");

    // when
    String result = promptBuilder.build("");

    // then
    assertThat(result).isEqualTo("입력: []");
  }

  @Test
  @DisplayName("입력 텍스트에 특수문자(개행/따옴표/백슬래시)가 포함되어도 그대로 치환된다")
  void build_특수문자_포함() throws IOException {
    // given
    setPromptTemplate("텍스트: %s");

    // when
    String result = promptBuilder.build("개행\n따옴표\"백슬래시\\포함");

    // then
    assertThat(result).isEqualTo("텍스트: 개행\n따옴표\"백슬래시\\포함");
  }
}
