package com.kusitms.kkium.notion.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.kusitms.kkium.global.config.AesEncryptConverter;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notion_connection")
public class NotionConnection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Convert(converter = AesEncryptConverter.class)
  @Column(name = "access_token", nullable = false)
  private String accessToken;

  @Column(name = "workspace_id")
  private String workspaceId;

  @Column(name = "workspace_name")
  private String workspaceName;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Builder
  public NotionConnection(
      String accessToken, String workspaceId, String workspaceName, Long userId) {
    this.accessToken = accessToken;
    this.workspaceId = workspaceId;
    this.workspaceName = workspaceName;
    this.userId = userId;
    this.createdAt = LocalDateTime.now();
  }

  public void updateToken(String accessToken, String workspaceId, String workspaceName) {
    this.accessToken = accessToken;
    this.workspaceId = workspaceId;
    this.workspaceName = workspaceName;
    this.createdAt = LocalDateTime.now();
  }
}
