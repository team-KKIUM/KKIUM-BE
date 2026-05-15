package com.kusitms.kkium.experience.domain;

import java.time.LocalDate;

import jakarta.persistence.*;

import com.kusitms.kkium.global.entity.BaseEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "educations")
@Entity
@Getter
@NoArgsConstructor
public class Education extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "organization_name", nullable = false)
  private String organizationName;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experience_id", nullable = false)
  private Experience experience;

  @Builder
  public Education(
      String organizationName,
      String name,
      LocalDate startDate,
      LocalDate endDate,
      Experience experience) {
    this.organizationName = organizationName;
    this.name = name;
    this.startDate = startDate;
    this.endDate = endDate;
    this.experience = experience;
  }
}
