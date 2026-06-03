package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_SCRAPE_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.dto.request.JdCreateRequest;
import com.kusitms.kkium.jd.dto.response.JdFetchResponse;
import com.kusitms.kkium.jd.utils.llm.LlmJdParser;
import com.kusitms.kkium.jd.utils.scrapper.JdWebScraper;
import com.kusitms.kkium.jd.utils.scrapper.PlaywrightJdScraper;

@ExtendWith(MockitoExtension.class)
class JdScrapServiceTest {

  @Mock private JdWebScraper jdWebScraper;

  @Mock private PlaywrightJdScraper playwrightJdScraper;

  @Mock private LlmJdParser llmJdParser;

  private JdScrapService jdScrapService;

  @BeforeEach
  void setUp() {
    jdScrapService = new JdScrapService(jdWebScraper, playwrightJdScraper, llmJdParser);
  }

  @Test
  @DisplayName("일반 공고 URL은 기본 스크래퍼 결과를 LLM 파서에 전달한다")
  void fetchJdWithDefaultScraper() {
    String url = "https://example.com/jobs/1";
    String rawText = "공고 본문";
    LlmJdParser.ParsedJd parsed =
        new LlmJdParser.ParsedJd(
            "백엔드 개발자", "끼움", "백엔드", "2026-06-01", "2026-06-30", List.of("지원 동기를 작성해주세요."), "공고 본문");

    when(jdWebScraper.scrape(url)).thenReturn(Map.of("rawText", rawText));
    when(llmJdParser.parse(rawText)).thenReturn(parsed);

    JdFetchResponse response = jdScrapService.fetchJd(new JdCreateRequest(url));

    assertThat(response.url()).isEqualTo(url);
    assertThat(response.postingTitle()).isEqualTo("백엔드 개발자");
    assertThat(response.questions()).containsExactly("지원 동기를 작성해주세요.");
    verify(playwrightJdScraper, never()).scrape(url);
  }

  @Test
  @DisplayName("CSR 사이트로 감지되면 Playwright 스크래퍼로 재시도한다")
  void retryWithPlaywrightWhenCsrDetected() {
    String url = "https://linkareer.com/activity/325758";
    String playwrightRawText = "Playwright로 가져온 공고 본문";
    LlmJdParser.ParsedJd parsed =
        new LlmJdParser.ParsedJd(
            "체험형 인턴", "링커리어", "서비스 기획", null, null, List.of(), playwrightRawText);

    when(jdWebScraper.scrape(url)).thenReturn(Map.of("rawText", "{}", "isCsr", "true"));
    when(playwrightJdScraper.scrape(url)).thenReturn(Map.of("rawText", playwrightRawText));
    when(llmJdParser.parse(playwrightRawText)).thenReturn(parsed);

    JdFetchResponse response = jdScrapService.fetchJd(new JdCreateRequest(url));

    assertThat(response.content()).isEqualTo(playwrightRawText);
    verify(playwrightJdScraper).scrape(url);
    verify(llmJdParser).parse(playwrightRawText);
  }

  @Test
  @DisplayName("rawText가 null이면 JD_SCRAPE_FAILED 예외를 던진다")
  void throwWhenRawTextIsNull() {
    String url = "https://example.com/jobs/empty";
    Map<String, String> scraped = new HashMap<>();
    scraped.put("rawText", null);

    when(jdWebScraper.scrape(url)).thenReturn(scraped);

    assertThatThrownBy(() -> jdScrapService.fetchJd(new JdCreateRequest(url)))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(JD_SCRAPE_FAILED);

    verify(llmJdParser, never()).parse(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("rawText가 blank이면 JD_SCRAPE_FAILED 예외를 던진다")
  void throwWhenRawTextIsBlank() {
    String url = "https://example.com/jobs/blank";

    when(jdWebScraper.scrape(url)).thenReturn(Map.of("rawText", "   "));

    assertThatThrownBy(() -> jdScrapService.fetchJd(new JdCreateRequest(url)))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(JD_SCRAPE_FAILED);

    verify(playwrightJdScraper, never()).scrape(url);
    verify(llmJdParser, never()).parse(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("사람인 relay URL은 실제 공고 URL로 정규화한다")
  void normalizeSaraminRelayUrl() {
    String relayUrl =
        "https://www.saramin.co.kr/zf_user/jobs/relay/view?isMypage=no&rec_idx=12345#seq=0";
    String normalizedUrl = "https://www.saramin.co.kr/zf_user/jobs/view?rec_idx=12345";
    String rawText = "사람인 공고 본문";
    LlmJdParser.ParsedJd parsed =
        new LlmJdParser.ParsedJd("사람인 공고", "사람인", "개발", null, null, List.of(), rawText);

    when(jdWebScraper.scrape(normalizedUrl)).thenReturn(Map.of("rawText", rawText));
    when(llmJdParser.parse(rawText)).thenReturn(parsed);

    JdFetchResponse response = jdScrapService.fetchJd(new JdCreateRequest(relayUrl));

    assertThat(response.url()).isEqualTo(normalizedUrl);
    verify(jdWebScraper).scrape(normalizedUrl);
  }
}
