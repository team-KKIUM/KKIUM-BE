package com.kusitms.kkium.resume.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.kusitms.kkium.jd.domain.JdAnswer;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "answer_experiences")
@Entity
@Getter
@NoArgsConstructor
public class AnswerExperience {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "answer_id", nullable = false)
  private JdAnswer jdAnswer;

  @Column(name = "experience_id", nullable = false)
  private Long experienceId;

  @Builder
  public AnswerExperience(JdAnswer jdAnswer, Long experienceId) {
    this.jdAnswer = jdAnswer;
    this.experienceId = experienceId;
  }
}
