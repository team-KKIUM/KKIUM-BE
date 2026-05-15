package com.kusitms.kkium.experience.service.analyze;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.experience.service.llm.LlmService;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.notion.domain.NotionConnection;
import com.kusitms.kkium.notion.repository.NotionConnectionRepository;
import com.kusitms.kkium.notion.utils.NotionApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceAnalyzeService {

  private final PdfAnalyzeService pdfAnalyzeService;
  private final NotionApiClient notionApiClient;
  private final NotionConnectionRepository notionConnectionRepository;
  private final LlmService llmService;

  public ExperienceAnalyzeResponse analyze(Long userId, MultipartFile file, String pageId) {
    validateAtLeastOne(file, pageId);

    StringBuilder combined = new StringBuilder();

    // PDF 텍스트 추출
    if (file != null && !file.isEmpty()) {
      String pdfText = pdfAnalyzeService.extractText(file);
      combined.append("[PDF 자료]\n").append(pdfText).append("\n\n");
    }

    // Notion 텍스트 추출
    if (pageId != null && !pageId.isBlank()) {
      String accessToken = getAccessToken(userId);
      String notionText = notionApiClient.getPageContent(accessToken, pageId);
      combined.append("[Notion 페이지]\n").append(notionText);
    }

    return llmService.analyze(userId, combined.toString());
  }

  private void validateAtLeastOne(MultipartFile file, String pageId) {
    boolean hasPdf = file != null && !file.isEmpty();
    boolean hasNotion = pageId != null && !pageId.isBlank();
    if (!hasPdf && !hasNotion) {
      throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private String getAccessToken(Long userId) {
    NotionConnection connection =
        notionConnectionRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BaseException(ErrorCode.NOTION_NOT_CONNECTED));
    return connection.getAccessToken();
  }
}
