package com.kusitms.kkium.experience.service;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfAnalyzeService {

  private static final String PDF_CONTENT_TYPE = "application/pdf";

  private final LlmService llmService;

  public ExperienceAnalyzeResponse analyzePdf(Long userId, MultipartFile file) {
    validatePdfFile(file);
    String extractedText = extractText(file);
    return llmService.analyze(userId, extractedText);
  }

  private void validatePdfFile(MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType == null || !contentType.equals(PDF_CONTENT_TYPE)) {
      throw new BaseException(ErrorCode.INVALID_FILE_TYPE);
    }
  }

  public String extractText(MultipartFile file) {
    try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(file.getInputStream()))) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(document);
    } catch (IOException e) {
      log.error("PDF 파싱 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.PDF_PARSE_FAILED);
    }
  }
}
