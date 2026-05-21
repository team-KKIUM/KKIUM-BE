package com.kusitms.kkium.user.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_PROFILE_COLOR;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional
  public void updateProfileColor(Long userId, Integer illustrateId) {
    if (illustrateId < 0 || illustrateId > 4) {
      throw new BaseException(INVALID_PROFILE_COLOR);
    }
    User user =
        userRepository.findById(userId).orElseThrow(() -> new BaseException(USER_NOT_FOUND));
    user.updateIllustrateId(illustrateId);
  }
}
