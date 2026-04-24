package com.kusitms.kkium.user.utils;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
    User user =
        userRepository
            .findById(Long.parseLong(id))
            .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

    return new CustomUserDetails(user.getId(), user.getName(), user.getRole().name());
  }
}
