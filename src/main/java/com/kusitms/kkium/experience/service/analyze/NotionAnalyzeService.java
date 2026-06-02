package com.kusitms.kkium.experience.service.analyze;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.experience.service.llm.LlmService;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.notion.domain.NotionConnection;
import com.kusitms.kkium.notion.dto.response.NotionPageListResponse;
import com.kusitms.kkium.notion.repository.NotionConnectionRepository;
import com.kusitms.kkium.notion.utils.NotionApiClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotionAnalyzeService {

  private final NotionApiClient notionApiClient;
  private final NotionConnectionRepository notionConnectionRepository;
  private final LlmService llmService;

  // 접근 가능한 Notion 페이지 목록 조회 (5분 캐싱)
  @Cacheable(value = "notion-pages", key = "#userId")
  public NotionPageListResponse getPages(Long userId) {
    NotionConnection connection = getConnection(userId);
    List<NotionPageListResponse.NotionPageInfo> pages =
        notionApiClient.getPages(connection.getAccessToken());
    return new NotionPageListResponse(connection.getWorkspaceName(), pages);
  }

  // 선택한 페이지 콘텐츠 추출 후 LLM 분석
  public ExperienceAnalyzeResponse analyze(Long userId, String pageId) {
    String accessToken = getConnection(userId).getAccessToken();
    String content = notionApiClient.getPageContent(accessToken, pageId);
    return llmService.analyze(userId, content);
  }

  private NotionConnection getConnection(Long userId) {
    return notionConnectionRepository
        .findByUserId(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.NOTION_NOT_CONNECTED));
  }
}
