package com.kusitms.kkium.experience.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.global.entity.BaseEntity;
import com.kusitms.kkium.user.domain.User;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "pieces")
@Entity
@Getter
@NoArgsConstructor
public class Piece extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private PieceType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "delete_at", nullable = true)
  private LocalDateTime deleteAt;

  @Builder
  public Piece(PieceType type, User user) {
    this.type = type;
    this.user = user;
  }

  public void delete() {
    this.deleteAt = LocalDateTime.now();
  }
}
