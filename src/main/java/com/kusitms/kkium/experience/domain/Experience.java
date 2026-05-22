package com.kusitms.kkium.experience.domain;

import jakarta.persistence.*;

import com.kusitms.kkium.global.entity.BaseEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "experiences")
@Entity
@Getter
@NoArgsConstructor
public class Experience extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "situation", columnDefinition = "TEXT")
  private String situation;

  @Column(name = "task", columnDefinition = "TEXT")
  private String task;

  @Column(name = "act", columnDefinition = "TEXT")
  private String act;

  @Column(name = "result", columnDefinition = "TEXT")
  private String result;

  @Column(name = "taken", columnDefinition = "TEXT")
  private String taken;

  @Column(name = "title")
  private String title;

  @Column(name = "one_line_intro")
  private String oneLineIntro;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "piece_id", nullable = false)
  private Piece piece;

  @Builder
  public Experience(
      String situation,
      String task,
      String act,
      String result,
      String taken,
      String title,
      String oneLineIntro,
      Piece piece) {
    this.situation = situation;
    this.task = task;
    this.act = act;
    this.result = result;
    this.taken = taken;
    this.title = title;
    this.oneLineIntro = oneLineIntro;
    this.piece = piece;
  }
}
