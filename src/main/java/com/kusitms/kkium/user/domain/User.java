package com.kusitms.kkium.user.domain;

import jakarta.persistence.*;

import com.kusitms.kkium.global.entity.BaseEntity;
import com.kusitms.kkium.user.domain.type.LoginType;
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

  @Column(name = "social_id", nullable = true)
  private String socialId;

  @Enumerated(EnumType.STRING)
  @Column(name = "login_type", nullable = true)
  private LoginType loginType;

  @Column(name = "illustrate_id", nullable = true)
  private Integer illustrateId;

  public void updateIllustrateId(Integer illustrateId) {
    this.illustrateId = illustrateId;
  }

  @Builder(builderMethodName = "basicLoginBuilder", builderClassName = "buildBasicLogin")
  public User(String name, String email, String password) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = Role.ROLE_ADMIN;
    this.illustrateId = 0;
  }

  @Builder(builderMethodName = "socialLoginBuilder", builderClassName = "buildSocialLogin")
  public static User socialLogin(String name, String socialId, LoginType loginType, String email) {
    User user = new User();
    user.name = name;
    user.socialId = socialId;
    user.loginType = loginType;
    user.email = email;
    user.role = Role.ROLE_USER;
    user.illustrateId = 0;
    return user;
  }
}
