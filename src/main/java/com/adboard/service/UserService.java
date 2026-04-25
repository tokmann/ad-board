package com.adboard.service;

import com.adboard.dto.request.user.ProfileUpdateRequestDto;
import com.adboard.dto.response.user.UserProfileDto;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  public UserProfileDto getMyProfile(Authentication authentication) {
    return null;
  }

  public UserProfileDto updateMyProfile(ProfileUpdateRequestDto request, Authentication authentication) {
    return null;
  }
}
