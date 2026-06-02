package com.kusitms.kkium.experience.service.llm.pipeline;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

  private static final String GEMINI_API_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

  private final WebClient webClient;
  private final ExperienceSchemaLoader schemaLoader;

  @Value("${gemini.api-key}")
  private String apiKey;

  public String call(String prompt) {
    Map<String, Object> requestBody =
        Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig",
                Map.of(
                    "response_mime_type",
                    "application/json",
                    "response_schema",
                    schemaLoader.get()));

    try {
      String response =
          webClient
              .post()
              .uri(GEMINI_API_URL + "?key=" + apiKey)
              .bodyValue(requestBody)
              .retrieve()
              .bodyToMono(String.class)
              .timeout(Duration.ofSeconds(25))
              .retryWhen(
                  Retry.backoff(3, Duration.ofSeconds(2))
                      .filter(
                          e ->
                              e instanceof WebClientResponseException we
                                  && (we.getStatusCode().is5xxServerError()
                                      || we.getStatusCode().value() == 429)))
              .block(Duration.ofSeconds(60));

      if (response == null) {
        log.error("Gemini API 응답이 null");
        throw new BaseException(ErrorCode.LLM_CALL_FAILED);
      }
      return response;
    } catch (Exception e) {
      log.error("Gemini API 호출 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_CALL_FAILED);
    }
  }
}
