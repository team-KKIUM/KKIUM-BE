package com.kusitms.kkium.experience.domain;

import java.time.LocalDate;

import jakarta.persistence.*;

import com.kusitms.kkium.global.entity.BaseEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "etcs")
@Entity
@Getter
@NoArgsConstructor
public class Etc extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experience_id", nullable = false)
  private Experience experience;

  @Builder
  public Etc(LocalDate startDate, LocalDate endDate, Experience experience) {
    this.startDate = startDate;
    this.endDate = endDate;
    this.experience = experience;
  }
}
