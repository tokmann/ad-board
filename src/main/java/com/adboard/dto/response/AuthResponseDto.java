package com.adboard.dto.response;

import lombok.Data;

@Data
public class AuthResponseDto {
  private String token;
  private UserPreviewDto user;
}