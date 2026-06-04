package com.kusitms.kkium.experience.service.llm.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

class ExperienceSchemaLoaderTest {

  private ExperienceSchemaLoader schemaLoader;

  @BeforeEach
  void setUp() {
    schemaLoader = new ExperienceSchemaLoader(new ObjectMapper());
  }

  @Test
  @DisplayName("classpath의 스키마 JSON을 정상적으로 로드한다")
  void init_정상_로드() throws IOException {
    // given
    String json = "{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\"}}}";
    Resource resource = new ByteArrayResource(json.getBytes());
    ReflectionTestUtils.setField(schemaLoader, "schemaResource", resource);

    // when
    schemaLoader.init();

    // then
    Map<String, Object> schema = schemaLoader.get();
    assertThat(schema).isNotNull().containsEntry("type", "object").containsKey("properties");
  }

  @Test
  @DisplayName("잘못된 JSON 형식이면 IOException을 던진다")
  void init_잘못된_JSON_예외() {
    // given
    String invalidJson = "{not a valid json";
    Resource resource = new ByteArrayResource(invalidJson.getBytes());
    ReflectionTestUtils.setField(schemaLoader, "schemaResource", resource);

    // when & then
    assertThatThrownBy(() -> schemaLoader.init()).isInstanceOf(IOException.class);
  }
}
