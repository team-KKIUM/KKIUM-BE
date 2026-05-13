package com.kusitms.kkium.notion.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.notion.domain.NotionConnection;
import com.kusitms.kkium.notion.dto.response.NotionTokenResponse;
import com.kusitms.kkium.notion.repository.NotionConnectionRepository;
import com.kusitms.kkium.notion.utils.NotionApiClient;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotionService {

    private final NotionApiClient notionApiClient;
    private final NotionConnectionRepository notionConnectionRepository;

    @Value("${notion.frontend-redirect-uri}")
    private String frontendRedirectUri;

    // OAuth 인증 URL 반환 (state에 userId 담아서 CSRF 방지 겸 사용자 식별)
    public String getAuthorizationUrl(Long userId) {
        String state = userId + ":" + UUID.randomUUID();
        return notionApiClient.getAuthorizationUrl(state);
    }

    // 콜백 처리 - access_token 저장 후 프론트 redirect URL 반환
    @Transactional
    public String handleCallback(String code, String state) {
        Long userId = parseUserIdFromState(state);

        NotionTokenResponse tokenResponse = notionApiClient.getAccessToken(code);

        notionConnectionRepository.findByUserId(userId)
                .ifPresentOrElse(
                        connection -> connection.updateToken(
                                tokenResponse.accessToken(),
                                tokenResponse.workspaceId(),
                                tokenResponse.workspaceName()
                        ),
                        () -> notionConnectionRepository.save(
                                NotionConnection.builder()
                                        .userId(userId)
                                        .accessToken(tokenResponse.accessToken())
                                        .workspaceId(tokenResponse.workspaceId())
                                        .workspaceName(tokenResponse.workspaceName())
                                        .build()
                        )
                );

        return frontendRedirectUri + "?success=true";
    }

    private Long parseUserIdFromState(String state) {
        try {
            return Long.parseLong(state.split(":")[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 state 값입니다.");
        }
    }
}
