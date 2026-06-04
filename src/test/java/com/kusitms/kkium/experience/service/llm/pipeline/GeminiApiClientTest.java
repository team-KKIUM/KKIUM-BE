package com.kusitms.kkium.experience.service.llm.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@ExtendWith(MockitoExtension.class)
class GeminiApiClientTest {

  private MockWebServer mockWebServer;
  private GeminiApiClient geminiApiClient;

  @Mock private ExperienceSchemaLoader schemaLoader;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    when(schemaLoader.get()).thenReturn(Map.of("type", "object"));

    WebClient webClient = WebClient.builder().build();
    geminiApiClient = new GeminiApiClient(webClient, schemaLoader);

    ReflectionTestUtils.setField(geminiApiClient, "apiKey", "test-api-key");
    ReflectionTestUtils.setField(
        geminiApiClient, "geminiApiUrl", mockWebServer.url("/gemini").toString());
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("정상 200 응답 시 응답 본문을 그대로 반환한다")
  void call_정상_응답() {
    String expectedBody = "{\"candidates\":[]}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(expectedBody));

    String result = geminiApiClient.call("test prompt");

    assertThat(result).isEqualTo(expectedBody);
    assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("4xx 응답 시 retry 없이 즉시 LLM_CALL_FAILED 예외를 던진다")
  void call_4xx_즉시_실패() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(400));

    assertThatThrownBy(() -> geminiApiClient.call("test prompt"))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LLM_CALL_FAILED);

    assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("5xx 응답 후 200 응답 시 retry를 통해 정상 결과를 반환한다")
  void call_5xx_후_retry_성공() {
    String expectedBody = "{\"candidates\":[]}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(expectedBody));

    String result = geminiApiClient.call("test prompt");

    assertThat(result).isEqualTo(expectedBody);
    assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("429 응답 후 200 응답 시 retry를 통해 정상 결과를 반환한다")
  void call_429_후_retry_성공() {
    String expectedBody = "{\"candidates\":[]}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(429));
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(expectedBody));

    String result = geminiApiClient.call("test prompt");

    assertThat(result).isEqualTo(expectedBody);
    assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
  }
}
