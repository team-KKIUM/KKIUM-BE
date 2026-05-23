package com.kusitms.kkium.jd.service;

import java.io.IOException;

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

  private final GoogleVisionOcrService googleVisionOcrService;

  public JdOcrResponse extractText(MultipartFile image) {
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
