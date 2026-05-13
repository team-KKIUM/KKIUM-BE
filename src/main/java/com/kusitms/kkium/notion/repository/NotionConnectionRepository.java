package com.kusitms.kkium.notion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.notion.domain.NotionConnection;

public interface NotionConnectionRepository extends JpaRepository<NotionConnection, Long> {

    Optional<NotionConnection> findByUserId(Long userId);
}
