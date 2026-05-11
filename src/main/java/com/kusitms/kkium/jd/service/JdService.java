package com.kusitms.kkium.jd.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.jd.dto.response.JdListPageResponse;
import com.kusitms.kkium.jd.dto.response.JdListResponse;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JdService {

  private final JdRepository jdRepository;

  public JdListPageResponse getJdList(CustomUserDetails userDetails, int page, int size) {
    Long userId = userDetails.getId();

    // sortOrder가 하나라도 있으면 사용자 정의 순서, 없으면 최신순
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
}
