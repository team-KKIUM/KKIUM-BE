package com.kusitms.kkium.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.domain.type.LoginType;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByIdAndDeleteAtIsNull(Long id);

  Optional<User> findByEmail(String email);

  Optional<User> findBySocialIdAndLoginType(String socialId, LoginType loginType);
}
