package com.kusitms.kkium.experience.service.llm.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExperienceSchemaLoader {

  private final ObjectMapper objectMapper;

  @Value("classpath:prompts/experience/analyze-schema.json")
  private Resource schemaResource;

  private Map<String, Object> schema;

  @PostConstruct
  public void init() throws IOException {
    try (InputStream inputStream = schemaResource.getInputStream()) {
      this.schema = objectMapper.readValue(inputStream, new TypeReference<>() {});
    }
  }

  public Map<String, Object> get() {
    return schema;
  }
}
