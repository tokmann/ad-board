package com.adboard.controller;

import com.adboard.dto.request.user.ProfileUpdateRequestDto;
import com.adboard.dto.response.user.UserProfileDto;
import com.adboard.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User Profile Management")
public class UserController {

  private final UserService userService;

  @Operation(summary = "Get current profile data")
  @GetMapping("/me")
  public ResponseEntity<UserProfileDto> getMyProfile(Authentication authentication) {
    log.info("Fetching profile for: {}", authentication.getName());
    UserProfileDto profile = userService.getMyProfile(authentication);
    return ResponseEntity.ok(profile);
  }

  @Operation(summary = "Edit profile")
  @PutMapping("/me")
  public ResponseEntity<UserProfileDto> updateMyProfile(
      @Valid @RequestBody ProfileUpdateRequestDto request,
      Authentication authentication) {
    log.info("Updating profile for: {}", authentication.getName());
    UserProfileDto updatedProfile = userService.updateMyProfile(request, authentication);
    log.info("Profile updated successfully for: {}", updatedProfile.getUsername());
    return ResponseEntity.ok(updatedProfile);
  }
}
