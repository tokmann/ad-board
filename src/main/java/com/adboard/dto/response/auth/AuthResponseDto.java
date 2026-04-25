package com.adboard.dto.response.auth;

import com.adboard.dto.response.user.UserProfileDto;
import lombok.Data;

@Data
public class AuthResponseDto {

  private String token;
  private UserProfileDto user;
}