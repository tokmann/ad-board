package com.adboard.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequestDto {

  @NotBlank(message = "Username is required")
  @Size(max = 50, message = "Username is too long")
  private String username;

  @NotBlank(message = "Phone is required")
  @Size(max = 20, message = "Phone number is too long")
  private String phone;

  @NotBlank(message = "City is required")
  @Size(max = 50, message = "City name is too long")
  private String city;
}
