package com.kusitms.kkium.jd.utils.ocr;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GoogleVisionOcrService {

  private static final String VISION_URL = "https://vision.googleapis.com/v1/images:annotate";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WebClient webClient;
  private final String apiKey;

  public GoogleVisionOcrService(
      WebClient webClient, @Value("${google.cloud.vision.api-key}") String apiKey) {
    this.webClient = webClient;
    this.apiKey = apiKey;
  }

  public String extractText(byte[] imageBytes) {
    String base64Image = Base64.getEncoder().encodeToString(imageBytes);

    Map<String, Object> body =
        Map.of(
            "requests",
            List.of(
                Map.of(
                    "image", Map.of("content", base64Image),
                    "features", List.of(Map.of("type", "DOCUMENT_TEXT_DETECTION")))));

    try {
      String raw =
          webClient
              .post()
              .uri(VISION_URL + "?key=" + apiKey)
              .bodyValue(body)
              .retrieve()
              .bodyToMono(String.class)
              .block(java.time.Duration.ofSeconds(30));

      return parseText(raw);
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      log.error("Google Vision API 호출 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.OCR_FAILED);
    }
  }

  private String parseText(String raw) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(raw);
      JsonNode text = root.path("responses").path(0).path("fullTextAnnotation").path("text");

      if (text.isMissingNode() || text.asText().isBlank()) {
        return "";
      }
      return text.asText();
    } catch (Exception e) {
      log.error("Google Vision 응답 파싱 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.OCR_FAILED);
    }
  }
}
