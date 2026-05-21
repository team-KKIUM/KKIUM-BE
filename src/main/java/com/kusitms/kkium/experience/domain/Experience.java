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

  @Column(name = "situation")
  private String situation;

  @Column(name = "task")
  private String task;

  @Column(name = "act")
  private String act;

  @Column(name = "result")
  private String result;

  @Column(name = "taken")
  private String taken;

  @Column(name = "title")
  private String title;

  @Column(name = "one_line_intro")
  private String oneLineIntro;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "piece_id", nullable = false)
  private Piece piece;

  public void updateTitle(String title) {
    this.title = title;
  }

  public void update(
      String title,
      String oneLineIntro,
      String situation,
      String task,
      String act,
      String result,
      String taken) {
    this.title = title;
    this.oneLineIntro = oneLineIntro;
    this.situation = situation;
    this.task = task;
    this.act = act;
    this.result = result;
    this.taken = taken;
  }

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
