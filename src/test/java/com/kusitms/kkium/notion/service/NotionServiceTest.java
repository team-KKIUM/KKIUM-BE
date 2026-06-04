package com.kusitms.kkium.notion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.notion.domain.NotionConnection;
import com.kusitms.kkium.notion.dto.response.NotionTokenResponse;
import com.kusitms.kkium.notion.repository.NotionConnectionRepository;
import com.kusitms.kkium.notion.utils.NotionApiClient;

@ExtendWith(MockitoExtension.class)
class NotionServiceTest {

  @Mock private NotionApiClient notionApiClient;
  @Mock private NotionConnectionRepository notionConnectionRepository;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private NotionService notionService;

  private static final Long USER_ID = 1L;
  private static final String ALLOWED_ORIGIN = "http://localhost:3000";

  @BeforeEach
  void setUp() {
    notionService = new NotionService(notionApiClient, notionConnectionRepository, redisTemplate);
    ReflectionTestUtils.setField(notionService, "allowedOrigins", List.of(ALLOWED_ORIGIN));
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  @DisplayName("허용된 origin이면 Redis에 state를 저장하고 인증 URL을 반환한다")
  void getAuthorizationUrl_허용된_origin() {
    when(notionApiClient.getAuthorizationUrl(anyString())).thenReturn("https://notion.so/auth");

    String result = notionService.getAuthorizationUrl(USER_ID, ALLOWED_ORIGIN);

    assertThat(result).isEqualTo("https://notion.so/auth");
    verify(valueOperations).set(anyString(), anyString(), eq(10L), eq(TimeUnit.MINUTES));
  }

  @Test
  @DisplayName("허용되지 않은 origin이면 ResponseStatusException을 던진다")
  void getAuthorizationUrl_허용안된_origin() {
    assertThatThrownBy(() -> notionService.getAuthorizationUrl(USER_ID, "http://evil.com"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("유효한 state로 콜백 처리 시 access_token을 저장하고 redirect URL을 반환한다")
  void handleCallback_정상_신규_저장() {
    String stateValue = "test-state-uuid";
    String state = USER_ID + ":" + stateValue + ":" + ALLOWED_ORIGIN;
    NotionTokenResponse tokenResponse =
        new NotionTokenResponse("access-token", "workspace-id", "워크스페이스");

    when(valueOperations.get("notion:state:" + stateValue))
        .thenReturn(USER_ID + ":" + ALLOWED_ORIGIN);
    when(redisTemplate.delete(anyString())).thenReturn(true);
    when(notionApiClient.getAccessToken(anyString())).thenReturn(tokenResponse);
    when(notionConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    String result = notionService.handleCallback("auth-code", state);

    assertThat(result).isEqualTo(ALLOWED_ORIGIN + "/experience/add?success=true");
    verify(notionConnectionRepository).save(any(NotionConnection.class));
  }

  @Test
  @DisplayName("만료된 state로 콜백 처리 시 NOTION_INVALID_STATE 예외를 던진다")
  void handleCallback_만료된_state() {
    String stateValue = "expired-state";
    String state = USER_ID + ":" + stateValue + ":" + ALLOWED_ORIGIN;

    when(valueOperations.get("notion:state:" + stateValue)).thenReturn(null);

    assertThatThrownBy(() -> notionService.handleCallback("code", state))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOTION_INVALID_STATE);
  }

  @Test
  @DisplayName("origin에 ':' 포함된 URL도 state에서 정확히 파싱된다")
  void handleCallback_origin_포트_포함_파싱() {
    // state = "1:uuid:http://localhost:3000" — split(":", 3)으로 파싱
    String stateValue = "test-uuid";
    String state = USER_ID + ":" + stateValue + ":" + ALLOWED_ORIGIN;
    NotionTokenResponse tokenResponse = new NotionTokenResponse("token", "ws-id", "ws-name");

    when(valueOperations.get("notion:state:" + stateValue))
        .thenReturn(USER_ID + ":" + ALLOWED_ORIGIN);
    when(redisTemplate.delete(anyString())).thenReturn(true);
    when(notionApiClient.getAccessToken(anyString())).thenReturn(tokenResponse);
    when(notionConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    String result = notionService.handleCallback("code", state);

    // 포트 포함 origin이 그대로 파싱됨
    assertThat(result).isEqualTo(ALLOWED_ORIGIN + "/experience/add?success=true");
  }
}
