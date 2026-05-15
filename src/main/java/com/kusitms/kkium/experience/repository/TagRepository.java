package com.kusitms.kkium.experience.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.experience.domain.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {}
