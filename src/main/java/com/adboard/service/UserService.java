package com.adboard.service;

import com.adboard.dto.mapper.UserMapper;
import com.adboard.dto.request.user.ProfileUpdateRequestDto;
import com.adboard.dto.response.user.UserProfileDto;
import com.adboard.entity.User;
import com.adboard.exception.ResourceNotFoundException;
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
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

    log.info("Profile data successfully retrieved for user: {}", user.getEmail());

    return userMapper.toProfileDto(user);
  }

  @Transactional
  public UserProfileDto updateMyProfile(ProfileUpdateRequestDto request, Authentication authentication) {
    String email = authentication.getName();
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

    userMapper.updateEntityFromDto(request, user);
    userRepository.save(user);

    log.info("Profile updated successfully for user: {}", email);

    return userMapper.toProfileDto(user);
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

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
