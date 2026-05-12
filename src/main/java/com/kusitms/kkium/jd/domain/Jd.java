package com.kusitms.kkium.jd.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.kusitms.kkium.global.entity.BaseEntity;
import com.kusitms.kkium.user.domain.User;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "jd")
@Entity
@Getter
@NoArgsConstructor
public class Jd extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "link_url")
  private String linkUrl;

  @Column(name = "additional_info")
  private String additionalInfo;

  @Column(name = "company_name")
  private String companyName;

  @Column(name = "recruitment_field")
  private String recruitmentField;

  @Column(name = "start_date")
  private LocalDateTime startDate;

  @Column(name = "end_date")
  private LocalDateTime endDate;

  @Column(name = "main_responsibilities")
  private String mainResponsibilities;

  @Column(name = "required_qualifications")
  private String requiredQualifications;

  @Column(name = "preferred_qualifications")
  private String preferredQualifications;

  @Column(name = "raw_text", columnDefinition = "TEXT")
  private String rawText;

  @Column(name = "analysis_snapshot", columnDefinition = "TEXT")
  private String analysisSnapshot;

  @Column(name = "hard_skill")
  private String hardSkill;

  @Column(name = "soft_skill")
  private String softSkill;

  @Column(name = "talent")
  private String talent;

  @Column(name = "is_target")
  private Boolean isTarget;

  @Column(name = "fitness", nullable = false)
  private Float fitness;

  @Column(name = "title")
  private String title;

  @Column(name = "delete_at")
  private LocalDateTime deleteAt;

  @Column(name = "embedding_vector")
  private String embeddingVector;

  @Column(name = "sort_order")
  private Integer sortOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Builder
  public Jd(
      String linkUrl,
      String additionalInfo,
      String companyName,
      String recruitmentField,
      LocalDateTime startDate,
      LocalDateTime endDate,
      String mainResponsibilities,
      String requiredQualifications,
      String preferredQualifications,
      String rawText,
      String analysisSnapshot,
      String hardSkill,
      String softSkill,
      String talent,
      Boolean isTarget,
      Float fitness,
      String title,
      String embeddingVector,
      User user) {
    this.linkUrl = linkUrl;
    this.additionalInfo = additionalInfo;
    this.companyName = companyName;
    this.recruitmentField = recruitmentField;
    this.startDate = startDate;
    this.endDate = endDate;
    this.mainResponsibilities = mainResponsibilities;
    this.requiredQualifications = requiredQualifications;
    this.preferredQualifications = preferredQualifications;
    this.rawText = rawText;
    this.analysisSnapshot = analysisSnapshot;
    this.hardSkill = hardSkill;
    this.softSkill = softSkill;
    this.talent = talent;
    this.isTarget = isTarget;
    this.fitness = fitness;
    this.title = title;
    this.embeddingVector = embeddingVector;
    this.user = user;
  }

  public void toggleTarget() {
    this.isTarget = !Boolean.TRUE.equals(this.isTarget);
  }
}
