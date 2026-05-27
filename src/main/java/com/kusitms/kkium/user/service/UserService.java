package com.kusitms.kkium.user.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_PROFILE_COLOR;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.dto.response.UserProfileResponse;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(Long userId) {
    return UserProfileResponse.from(getActiveUser(userId));
  }

  @Transactional
  public void updateProfileColor(Long userId, Integer illustrateId) {
    if (illustrateId < 0 || illustrateId > 4) {
      throw new BaseException(INVALID_PROFILE_COLOR);
    }
    User user = getActiveUser(userId);
    user.updateIllustrateId(illustrateId);
  }

  @Transactional
  public void delete(Long userId) {
    getActiveUser(userId).delete();
  }

  @Transactional
  public void agreeTerms(Long userId) {
    getActiveUser(userId).agreeTerms();
  }

  private User getActiveUser(Long userId) {
    return userRepository
        .findByIdAndDeleteAtIsNull(userId)
        .orElseThrow(() -> new BaseException(USER_NOT_FOUND));
  }
}
