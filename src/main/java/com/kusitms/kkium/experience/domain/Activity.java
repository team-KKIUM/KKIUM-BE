package com.kusitms.kkium.experience.domain;

import java.time.LocalDate;

import jakarta.persistence.*;

import com.kusitms.kkium.global.entity.BaseEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "activities")
@Entity
@Getter
@NoArgsConstructor
public class Activity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "team_num", nullable = false)
  private Integer teamNum;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "contribution_rate", nullable = false)
  private Integer contributionRate;

  @Column(name = "role", nullable = false)
  private String role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experience_id", nullable = false)
  private Experience experience;

  @Builder
  public Activity(
      String name,
      Integer teamNum,
      LocalDate startDate,
      LocalDate endDate,
      Integer contributionRate,
      String role,
      Experience experience) {
    this.name = name;
    this.teamNum = teamNum;
    this.startDate = startDate;
    this.endDate = endDate;
    this.contributionRate = contributionRate;
    this.role = role;
    this.experience = experience;
  }
}
