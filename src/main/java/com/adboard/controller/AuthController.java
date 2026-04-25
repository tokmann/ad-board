package com.adboard.controller;

import com.adboard.dto.request.auth.LoginRequestDto;
import com.adboard.dto.request.auth.RegisterRequestDto;
import com.adboard.dto.response.auth.AuthResponseDto;
import com.adboard.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration and login")
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "New user registration")
  @PostMapping("/register")
  public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
    log.info("Registration request for user: {}", request.getUsername());
    AuthResponseDto response = authService.register(request);
    log.info("User registered successfully: {}", response.getUser().getUsername());
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Login")
  @PostMapping("/login")
  public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
    log.info("Login attempt for email: {}", request.getEmail());
    AuthResponseDto response = authService.login(request);
    log.info("User logged in successfully: {}", response.getUser().getUsername());
    return ResponseEntity.ok(response);
  }
}
