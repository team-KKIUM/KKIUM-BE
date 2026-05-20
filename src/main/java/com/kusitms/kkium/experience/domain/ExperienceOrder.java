package com.kusitms.kkium.experience.domain;

import jakarta.persistence.*;

import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.user.domain.User;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
    name = "experience_order",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_experience_order",
          columnNames = {"user_id", "experience_id", "piece_type"})
    })
@Entity
@Getter
@NoArgsConstructor
public class ExperienceOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;

  @Enumerated(EnumType.STRING)
  @Column(name = "piece_type", nullable = false)
  private PieceType pieceType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experience_id", nullable = false)
  private Experience experience;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Builder
  public ExperienceOrder(Integer sortOrder, PieceType pieceType, Experience experience, User user) {
    this.sortOrder = sortOrder;
    this.pieceType = pieceType;
    this.experience = experience;
    this.user = user;
  }

  public void updateSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }
}
