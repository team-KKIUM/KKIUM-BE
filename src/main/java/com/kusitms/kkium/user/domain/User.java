package com.kusitms.kkium.user.domain;

import jakarta.persistence.*;

import com.kusitms.kkium.global.entity.BaseEntity;
import com.kusitms.kkium.user.domain.type.JobType;
import com.kusitms.kkium.user.domain.type.Role;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "users")
@Entity
@Getter
@NoArgsConstructor
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private Role role;

  @Column(name = "email", nullable = true, unique = true)
  private String email;

  @Column(name = "password", nullable = true)
  private String password;

  @Column(name = "kakao_id", nullable = true, unique = true)
  private Long kakaoId;

  @Column(name = "illustrate_id", nullable = true)
  private Integer illustrateId;

  @Enumerated(EnumType.STRING)
  @Column(name = "job_type", nullable = true)
  private JobType jobType;

  public void updateIllustrateId(Integer illustrateId) {
    this.illustrateId = illustrateId;
  }

  public void updateJobType(JobType jobType) {
    this.jobType = jobType;
  }

  @Builder(builderMethodName = "basicLoginBuilder", builderClassName = "buildBasicLogin")
  public User(String name, String email, String password) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = Role.ROLE_ADMIN;
    this.illustrateId = 0;
  }

  @Builder(builderMethodName = "kakaoLoginBuilder", builderClassName = "buildKakaoLogin")
  public User(String name, Long kakaoId, String email) {
    this.name = name;
    this.kakaoId = kakaoId;
    this.email = email;
    this.role = Role.ROLE_USER;
    this.illustrateId = 0;
  }
}
