package com.kusitms.kkium.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.user.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {}
