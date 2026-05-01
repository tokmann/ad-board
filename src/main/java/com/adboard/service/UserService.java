package com.adboard.service;

import com.adboard.dto.mapper.UserMapper;
import com.adboard.dto.request.user.ProfileUpdateRequestDto;
import com.adboard.dto.response.user.UserProfileDto;
import com.adboard.entity.User;
import com.adboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Transactional(readOnly = true)
  public UserProfileDto getMyProfile(Authentication authentication) {
    String email = authentication.getName();

    log.debug("Loading profile for authenticated user: {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.error("Authenticated user not found in DB: {}", email);
          return new IllegalArgumentException("User not found: " + email);
        });
    return userMapper.toProfileDto(user);
  }

  @Transactional
  public UserProfileDto updateMyProfile(ProfileUpdateRequestDto request, Authentication authentication) {
    String email = authentication.getName();

    log.info("Updating profile for user: {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.error("Authenticated user not found in DB: {}", email);
          return new IllegalArgumentException("User not found: " + email);
        });

    userMapper.updateEntityFromDto(request, user);
    userRepository.save(user);

    log.info("Profile updated successfully for user: {}", email);

    return userMapper.toProfileDto(user);
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("User not found by email: {}", email);
          return new UsernameNotFoundException("User not found: " + email);
        });
    var authorities = user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority(role.getName()))
        .toList();

    return new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPasswordHash(),
        authorities
    );
  }
}
