package com.kusitms.kkium.notion.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.NOTION_INVALID_STATE;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.notion.domain.NotionConnection;
import com.kusitms.kkium.notion.dto.response.NotionTokenResponse;
import com.kusitms.kkium.notion.repository.NotionConnectionRepository;
import com.kusitms.kkium.notion.utils.NotionApiClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotionService {

  private final NotionApiClient notionApiClient;
  private final NotionConnectionRepository notionConnectionRepository;
  private final StringRedisTemplate redisTemplate;

  private static final String STATE_PREFIX = "notion:state:";
  private static final long STATE_TTL_MINUTES = 10;

  @Value("${notion.frontend-redirect-uri}")
  private String frontendRedirectUri;

  // OAuth 인증 URL 반환 (state Redis 저장 후 CSRF 검증용)
  public String getAuthorizationUrl(Long userId) {
    String stateValue = UUID.randomUUID().toString();
    String state = userId + ":" + stateValue;
    redisTemplate.opsForValue().set(STATE_PREFIX + stateValue, String.valueOf(userId), STATE_TTL_MINUTES, TimeUnit.MINUTES);
    return notionApiClient.getAuthorizationUrl(state);
  }

  // 콜백 처리 - state 검증 후 access_token 저장 및 프론트 redirect URL 반환
  @Transactional
  public String handleCallback(String code, String state) {
    validateState(state);
    Long userId = parseUserIdFromState(state);

    NotionTokenResponse tokenResponse = notionApiClient.getAccessToken(code);

    notionConnectionRepository
        .findByUserId(userId)
        .ifPresentOrElse(
            connection ->
                connection.updateToken(
                    tokenResponse.accessToken(),
                    tokenResponse.workspaceId(),
                    tokenResponse.workspaceName()),
            () ->
                notionConnectionRepository.save(
                    NotionConnection.builder()
                        .userId(userId)
                        .accessToken(tokenResponse.accessToken())
                        .workspaceId(tokenResponse.workspaceId())
                        .workspaceName(tokenResponse.workspaceName())
                        .build()));

    return frontendRedirectUri + "?success=true";
  }

  private void validateState(String state) {
    String stateValue = state.split(":")[1];
    String savedUserId = redisTemplate.opsForValue().get(STATE_PREFIX + stateValue);
    if (savedUserId == null) {
      throw new BaseException(NOTION_INVALID_STATE);
    }
    redisTemplate.delete(STATE_PREFIX + stateValue);
  }

  private Long parseUserIdFromState(String state) {
    try {
      return Long.parseLong(state.split(":")[0]);
    } catch (Exception e) {
      throw new BaseException(NOTION_INVALID_STATE);
    }
  }
}
