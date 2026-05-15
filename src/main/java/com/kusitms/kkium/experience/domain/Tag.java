package com.kusitms.kkium.experience.domain;

import jakarta.persistence.*;

import com.kusitms.kkium.experience.domain.type.TagCategory;
import com.kusitms.kkium.global.entity.BaseEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "tags")
@Entity
@Getter
@NoArgsConstructor
public class Tag extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false)
  private TagCategory category;

  @Column(name = "field", nullable = false)
  private String field;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experience_id", nullable = false)
  private Experience experience;

  @Builder
  public Tag(TagCategory category, String field, Experience experience) {
    this.category = category;
    this.field = field;
    this.experience = experience;
  }
}
