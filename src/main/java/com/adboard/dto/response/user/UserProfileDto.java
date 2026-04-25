package com.adboard.dto.response.user;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserProfileDto {

  private Long id;
  private String username;
  private String email;
  private String phone;
  private String city;
  private LocalDateTime createdAt;
  private Set<String> roles;
}
