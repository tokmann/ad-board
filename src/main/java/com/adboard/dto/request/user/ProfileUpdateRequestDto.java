package com.adboard.dto.request.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequestDto {

  @Size(max = 20, message = "Phone number is too long")
  private String phone;

  @Size(max = 50, message = "City name is too long")
  private String city;
}
