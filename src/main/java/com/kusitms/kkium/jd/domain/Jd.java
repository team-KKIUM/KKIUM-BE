package com.kusitms.kkium.jd.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.kusitms.kkium.global.entity.BaseEntity;
import com.kusitms.kkium.jd.domain.type.AnalysisStatus;
import com.kusitms.kkium.user.domain.User;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "jds")
@Entity
@Getter
@NoArgsConstructor
public class Jd extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "link_url")
  private String linkUrl;

  @Column(name = "posting_title")
  private String postingTitle;

  @Column(name = "company_name")
  private String companyName;

  @Column(name = "recruitment_field")
  private String recruitmentField;

  @Column(name = "start_date")
  private LocalDateTime startDate;

  @Column(name = "end_date")
  private LocalDateTime endDate;

  @Column(name = "raw_text", columnDefinition = "TEXT")
  private String rawText;

  @Column(name = "main_responsibilities", columnDefinition = "TEXT")
  private String mainResponsibilities;

  @Column(name = "required_qualifications", columnDefinition = "TEXT")
  private String requiredQualifications;

  @Column(name = "preferred_qualifications", columnDefinition = "TEXT")
  private String preferredQualifications;

  @Column(name = "hard_skill", columnDefinition = "TEXT")
  private String hardSkill;

  @Column(name = "soft_skill", columnDefinition = "TEXT")
  private String softSkill;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "analysis_status",
      nullable = false,
      columnDefinition = "varchar(255) default 'PENDING'")
  private AnalysisStatus analysisStatus;

  @Column(name = "application_fit_score")
  private Integer applicationFitScore;

  @Column(name = "is_target")
  private Boolean isTarget;

  @Column(name = "sort_order")
  private Integer sortOrder;

  @Column(name = "delete_at")
  private LocalDateTime deleteAt;

  public void update(
      String postingTitle,
      String companyName,
      String recruitmentField,
      LocalDateTime startDate,
      LocalDateTime endDate) {
    this.postingTitle = postingTitle;
    this.companyName = companyName;
    this.recruitmentField = recruitmentField;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public void updateAnalysisStatus(AnalysisStatus status) {
    this.analysisStatus = status;
  }

  public void updateAnalysis(
      String mainResponsibilities,
      String requiredQualifications,
      String preferredQualifications,
      String hardSkill,
      String softSkill) {
    this.mainResponsibilities = mainResponsibilities;
    this.requiredQualifications = requiredQualifications;
    this.preferredQualifications = preferredQualifications;
    this.hardSkill = hardSkill;
    this.softSkill = softSkill;
  }

  public void updateApplicationFitScore(int score) {
    this.applicationFitScore = score;
  }

  public void updateTitle(String title) {
    this.postingTitle = title;
  }

  public void updateSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  public void toggleTarget() {
    this.isTarget = !Boolean.TRUE.equals(this.isTarget);
  }

  public void delete() {
    this.deleteAt = LocalDateTime.now();
  }

  @Builder
  public Jd(
      User user,
      String linkUrl,
      String postingTitle,
      String companyName,
      String recruitmentField,
      LocalDateTime startDate,
      LocalDateTime endDate,
      String rawText,
      String mainResponsibilities,
      String requiredQualifications,
      String preferredQualifications,
      String hardSkill,
      String softSkill,
      Boolean isTarget) {
    this.analysisStatus = AnalysisStatus.PENDING;
    this.user = user;
    this.linkUrl = linkUrl;
    this.postingTitle = postingTitle;
    this.companyName = companyName;
    this.recruitmentField = recruitmentField;
    this.startDate = startDate;
    this.endDate = endDate;
    this.rawText = rawText;
    this.mainResponsibilities = mainResponsibilities;
    this.requiredQualifications = requiredQualifications;
    this.preferredQualifications = preferredQualifications;
    this.hardSkill = hardSkill;
    this.softSkill = softSkill;
    this.isTarget = isTarget;
  }
}
