package com.kusitms.kkium.user.domain;

import jakarta.persistence.*;

import com.kusitms.kkium.global.entity.BaseEntity;
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

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "password", nullable = false)
  private String password;

  @Builder(builderMethodName = "basicLoginBuilder", builderClassName = "buildBasicLogin")
  public User(String name, String email, String password) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = Role.ROLE_ADMIN;
  }
}
