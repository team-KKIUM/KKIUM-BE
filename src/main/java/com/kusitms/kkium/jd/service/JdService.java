package com.kusitms.kkium.jd.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.dto.response.JdListPageResponse;
import com.kusitms.kkium.jd.dto.response.JdListResponse;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JdService {

  private static final int MAX_TARGET_COUNT = 5;

  private final JdRepository jdRepository;

  public JdListPageResponse getJdList(CustomUserDetails userDetails, int page, int size) {
    Long userId = userDetails.getId();

    boolean hasSortOrder =
        jdRepository.existsByUserIdAndDeleteAtIsNullAndSortOrderIsNotNull(userId);

    Pageable pageable =
        hasSortOrder
            ? PageRequest.of(page, size, Sort.by("sortOrder").ascending())
            : PageRequest.of(page, size, Sort.by("createdDate").descending());

    Page<JdListResponse> result =
        jdRepository.findByUserIdAndDeleteAtIsNull(userId, pageable).map(JdListResponse::from);

    return new JdListPageResponse(
        result.getContent(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.hasNext());
  }

  @Transactional
  public void toggleTarget(Long jdId, CustomUserDetails userDetails) {
    Long userId = userDetails.getId();

    Jd jd =
        jdRepository
            .findByIdAndDeleteAtIsNull(jdId)
            .orElseThrow(() -> new BaseException(ErrorCode.JD_NOT_FOUND));

    // 5개 제한 체크
    if (!Boolean.TRUE.equals(jd.getIsTarget())) {
      long targetCount = jdRepository.countByUserIdAndIsTargetTrueAndDeleteAtIsNull(userId);
      if (targetCount >= MAX_TARGET_COUNT) {
        throw new BaseException(ErrorCode.JD_TARGET_LIMIT_EXCEEDED);
      }
    }

    jd.toggleTarget();
  }

  @Transactional
  public void deleteJd(Long jdId, CustomUserDetails userDetails) {
    Long userId = userDetails.getId();

    Jd jd =
        jdRepository
            .findByIdAndDeleteAtIsNull(jdId)
            .orElseThrow(() -> new BaseException(ErrorCode.JD_NOT_FOUND));

    if (!jd.getUser().getId().equals(userId)) {
      throw new BaseException(ErrorCode.FORBIDDEN);
    }

    jd.delete();
  }
}
