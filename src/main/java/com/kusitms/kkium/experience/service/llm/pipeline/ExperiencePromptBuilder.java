package com.kusitms.kkium.experience.service.llm.pipeline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class ExperiencePromptBuilder {

  @Value("classpath:prompts/experience/analyze-prompt.txt")
  private Resource promptResource;

  private String promptTemplate;

  @PostConstruct
  public void init() throws IOException {
    this.promptTemplate =
        StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);
  }

  public String build(String extractedText) {
    return promptTemplate.formatted(extractedText);
  }
}
