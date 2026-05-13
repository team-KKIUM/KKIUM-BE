package com.kusitms.kkium.jd.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.kusitms.kkium.global.entity.BaseEntity;
import com.kusitms.kkium.user.domain.User;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
    name = "jd_answers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"jd_question_id", "user_id"}))
@Entity
@Getter
@NoArgsConstructor
public class JdAnswer extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "jd_question_id", nullable = false)
  private JdQuestion jdQuestion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "content", columnDefinition = "TEXT", nullable = false)
  private String content;

  @Column(name = "ai_draft", columnDefinition = "TEXT")
  private String aiDraft;

  @Builder
  public JdAnswer(JdQuestion jdQuestion, User user, String content) {
    this.jdQuestion = jdQuestion;
    this.user = user;
    this.content = content;
  }

  public void updateContent(String content) {
    this.content = content;
  }
}
