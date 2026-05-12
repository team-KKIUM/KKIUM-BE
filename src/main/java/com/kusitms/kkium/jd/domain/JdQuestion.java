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

import com.kusitms.kkium.global.entity.BaseEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "jd_questions")
@Entity
@Getter
@NoArgsConstructor
public class JdQuestion extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "jd_id", nullable = false)
  private Jd jd;

  @Column(name = "order_num", nullable = false)
  private Integer orderNum;

  @Column(name = "content", columnDefinition = "TEXT", nullable = false)
  private String content;

  public void updateContent(String content) {
    this.content = content;
  }

  @Builder
  public JdQuestion(Jd jd, Integer orderNum, String content) {
    this.jd = jd;
    this.orderNum = orderNum;
    this.content = content;
  }
}
