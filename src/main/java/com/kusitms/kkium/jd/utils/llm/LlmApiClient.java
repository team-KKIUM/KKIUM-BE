package com.kusitms.kkium.jd.utils.llm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/** OpenAI API 호출만 담당하는 클라이언트 */
@Slf4j
@Component
public class LlmApiClient {

  private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
  private static final String MODEL = "gpt-4o";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WebClient webClient;
  private final String apiKey;

  public LlmApiClient(WebClient webClient, @Value("${openai.api.key}") String apiKey) {
    this.webClient = webClient;
    this.apiKey = apiKey;
  }

  /**
   * OpenAI API를 호출하고 응답 content 문자열을 반환한다.
   *
   * @throws BaseException LLM_CALL_FAILED — 4xx/5xx/네트워크 오류
   */
  public String call(String prompt, Map<String, Object> jsonSchema) {
    Map<String, Object> body = new HashMap<>();
    body.put("model", MODEL);
    body.put("temperature", 0);
    body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
    body.put("response_format", Map.of("type", "json_schema", "json_schema", jsonSchema));

    try {
      String response =
          webClient
              .post()
              .uri(OPENAI_URL)
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + apiKey)
              .bodyValue(body)
              .retrieve()
              .onStatus(
                  HttpStatusCode::is4xxClientError,
                  clientResponse -> {
                    log.warn("OpenAI 클라이언트 에러: {}", clientResponse.statusCode());
                    return clientResponse.createException();
                  })
              .onStatus(
                  HttpStatusCode::is5xxServerError,
                  clientResponse -> {
                    log.warn("OpenAI 서버 에러: {}", clientResponse.statusCode());
                    return clientResponse.createException();
                  })
              .bodyToMono(String.class)
              .block();

      JsonNode root = OBJECT_MAPPER.readTree(response);
      return root.path("choices").path(0).path("message").path("content").asText();
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      log.warn("OpenAI 호출 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_CALL_FAILED);
    }
  }
}
