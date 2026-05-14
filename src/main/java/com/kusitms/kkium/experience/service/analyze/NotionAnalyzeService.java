package com.kusitms.kkium.experience.service.analyze;

import java.util.List;

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

  // 접근 가능한 Notion 페이지 목록 조회
  public NotionPageListResponse getPages(Long userId) {
    String accessToken = getAccessToken(userId);
    List<NotionPageListResponse.NotionPageInfo> pages = notionApiClient.getPages(accessToken);
    return new NotionPageListResponse(pages);
  }

  // 선택한 페이지 콘텐츠 추출 후 LLM 분석
  public ExperienceAnalyzeResponse analyze(Long userId, String pageId) {
    String accessToken = getAccessToken(userId);
    String content = notionApiClient.getPageContent(accessToken, pageId);
    return llmService.analyze(userId, content);
  }

  private String getAccessToken(Long userId) {
    NotionConnection connection =
        notionConnectionRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BaseException(ErrorCode.NOTION_NOT_CONNECTED));
    return connection.getAccessToken();
  }
}
