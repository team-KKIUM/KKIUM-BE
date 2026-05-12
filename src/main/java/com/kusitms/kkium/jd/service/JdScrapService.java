package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_SCRAPE_FAILED;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.dto.request.JdCreateRequest;
import com.kusitms.kkium.jd.dto.response.JdFetchResponse;
import com.kusitms.kkium.jd.utils.JdWebScraper;
import com.kusitms.kkium.jd.utils.LlmJdParser;
import com.kusitms.kkium.jd.utils.PlaywrightJdScraper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JdScrapService {

  private final JdWebScraper jdWebScraper;
  private final PlaywrightJdScraper playwrightJdScraper;
  private final LlmJdParser llmJdParser;

  public JdFetchResponse fetchJd(JdCreateRequest request) {
    String url = normalizeUrl(request.linkUrl());

    Map<String, String> scraped = jdWebScraper.scrape(url);

    if (isCsr(scraped)) {
      log.info("CSR 사이트 감지, Playwright로 재시도: {}", url);
      scraped = new HashMap<>(scraped);
      scraped.putAll(playwrightJdScraper.scrape(url));
    }

    String rawText = scraped.get("rawText");
    if (rawText == null || rawText.isBlank()) {
      throw new BaseException(JD_SCRAPE_FAILED);
    }

    return JdFetchResponse.from(llmJdParser.parse(rawText), url);
  }

  private boolean isCsr(Map<String, String> scraped) {
    String rawText = scraped.getOrDefault("rawText", "").trim();
    if (rawText.startsWith("{") || rawText.startsWith("[")) return true;
    return "true".equals(scraped.get("isCsr"));
  }

  private String normalizeUrl(String url) {
    try {
      String decoded = java.net.URLDecoder.decode(url, java.nio.charset.StandardCharsets.UTF_8);
      int fragmentIdx = decoded.indexOf('#');
      String cleaned = fragmentIdx >= 0 ? decoded.substring(0, fragmentIdx) : decoded;

      // 사람인 relay URL → 실제 공고 URL로 변환
      if (cleaned.contains("saramin.co.kr") && cleaned.contains("/relay/")) {
        java.net.URI uri = java.net.URI.create(cleaned);
        String query = uri.getQuery();
        if (query != null) {
          for (String param : query.split("&")) {
            if (param.startsWith("rec_idx=")) {
              String recIdx = param.substring("rec_idx=".length());
              return "https://www.saramin.co.kr/zf_user/jobs/view?rec_idx=" + recIdx;
            }
          }
        }
      }

      return cleaned;
    } catch (Exception e) {
      return url;
    }
  }
}
