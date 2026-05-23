package com.kusitms.kkium.jd.service;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.jd.dto.response.JdOcrResponse;
import com.kusitms.kkium.jd.utils.ocr.GoogleVisionOcrService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JdOcrService {

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/png", "image/jpg", "image/jpeg");

  private final GoogleVisionOcrService googleVisionOcrService;

  public JdOcrResponse extractText(MultipartFile image) {
    String contentType = image.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new BaseException(ErrorCode.INVALID_IMAGE_TYPE);
    }

    byte[] imageBytes;
    try {
      imageBytes = image.getBytes();
    } catch (IOException e) {
      throw new BaseException(ErrorCode.OCR_FAILED);
    }

    String text = googleVisionOcrService.extractText(imageBytes);
    return new JdOcrResponse(text);
  }
}
