package com.kusitms.kkium.global.utils;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LlmEmbeddingService {

  private static final String OPENAI_URL = "https://api.openai.com/v1/embeddings";
  private static final String EMBEDDING_MODEL = "text-embedding-3-small";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WebClient webClient;
  private final String apiKey;

  public LlmEmbeddingService(WebClient webClient, @Value("${openai.api.key}") String apiKey) {
    this.webClient = webClient;
    this.apiKey = apiKey;
  }

  public float[] embed(String text) {
    if (text == null || text.isBlank()) return null;

    Map<String, Object> body = Map.of("model", EMBEDDING_MODEL, "input", text);

    try {
      String response =
          webClient
              .post()
              .uri(OPENAI_URL)
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .bodyValue(body)
              .retrieve()
              .bodyToMono(String.class)
              .block();

      return extractEmbedding(response);
    } catch (Exception e) {
      log.warn("임베딩 생성 실패: {}", e.getMessage());
      return null;
    }
  }

  private float[] extractEmbedding(String response) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(response);
      JsonNode embeddingNode = root.path("data").get(0).path("embedding");

      float[] embedding = new float[embeddingNode.size()];
      for (int i = 0; i < embeddingNode.size(); i++) {
        embedding[i] = (float) embeddingNode.get(i).asDouble();
      }
      return embedding;
    } catch (Exception e) {
      log.warn("임베딩 응답 파싱 실패: {}", e.getMessage());
      return null;
    }
  }
}
