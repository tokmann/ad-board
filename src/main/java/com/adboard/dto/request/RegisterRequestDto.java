package com.adboard.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDto {

  @NotBlank(message = "Username is required")
  @Size(max = 50, message = "Username length must be less than 50 characters")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email format is invalid")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 6, message = "Password length must be 6 or more characters")
  private String password;

  @NotBlank(message = "Phone is required")
  @Size(max = 20)
  private String phone;

  @NotBlank(message = "City is required")
  @Size(max = 50)
  private String city;
}
