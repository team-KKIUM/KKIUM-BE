package com.kusitms.kkium.experience.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.experience.domain.Activity;

public interface ActivityRepository extends JpaRepository<Activity, Long> {}
