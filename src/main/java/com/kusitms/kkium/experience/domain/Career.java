package com.kusitms.kkium.experience.domain;

import java.time.LocalDate;

import jakarta.persistence.*;

import com.kusitms.kkium.global.entity.BaseEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "careers")
@Entity
@Getter
@NoArgsConstructor
public class Career extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "company", nullable = false)
  private String company;

  @Column(name = "employment_status", nullable = false)
  private String employmentStatus;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experience_id", nullable = false)
  private Experience experience;

  @Builder
  public Career(
      String name,
      String company,
      String employmentStatus,
      LocalDate startDate,
      LocalDate endDate,
      Experience experience) {
    this.name = name;
    this.company = company;
    this.employmentStatus = employmentStatus;
    this.startDate = startDate;
    this.endDate = endDate;
    this.experience = experience;
  }

  public void update(
      String name,
      String company,
      String employmentStatus,
      LocalDate startDate,
      LocalDate endDate) {
    this.name = name;
    this.company = company;
    this.employmentStatus = employmentStatus;
    this.startDate = startDate;
    this.endDate = endDate;
  }
}
