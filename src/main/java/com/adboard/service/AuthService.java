package com.adboard.service;

import com.adboard.dto.mapper.UserMapper;
import com.adboard.dto.request.auth.LoginRequestDto;
import com.adboard.dto.request.auth.RegisterRequestDto;
import com.adboard.dto.response.auth.AuthResponseDto;
import com.adboard.dto.response.user.UserProfileDto;
import com.adboard.entity.User;
import com.adboard.entity.reference.Role;
import com.adboard.exception.UserAlreadyExistsException;
import com.adboard.repository.RoleRepository;
import com.adboard.repository.UserRepository;
import com.adboard.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@PropertySource("classpath:application.properties")
public class AuthService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserService userService;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationManager authenticationManager;
  @Value("${app.security.admin-secret}")
  private String adminSecret;

  @Transactional
  public AuthResponseDto register(RegisterRequestDto request) {
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
    }
    if (userRepository.findByUsername(request.getUsername()).isPresent()) {
      throw new UserAlreadyExistsException("Username already taken: " + request.getUsername());
    }

    HashSet<Role> roles = new HashSet<>();

    if (request.getAdminSecretToken() != null) {
      if (!request.getAdminSecretToken().equals(adminSecret)) {
        throw new IllegalArgumentException("Invalid admin secret token");
      }
      Role adminRole = roleRepository.findByName("ROLE_ADMIN")
          .orElseThrow(() -> new IllegalStateException("Configuration error: default role 'ROLE_ADMIN' missing"));
      roles.add(adminRole);
    }

    Role userRole = roleRepository.findByName("ROLE_USER")
        .orElseThrow(() -> new IllegalStateException("Configuration error: default role 'ROLE_USER' missing"));

    roles.add(userRole);
    User user = userMapper.toEntity(request);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setRoles(roles);
    userRepository.save(user);

    log.info("User registered: id={}, email={}", user.getId(), user.getEmail());

    Authentication authentication = createAuthentication(user);
    String token = jwtTokenProvider.generateToken(authentication);
    UserProfileDto profileDto = userMapper.toProfileDto(user);
    AuthResponseDto response = new AuthResponseDto();
    response.setUser(profileDto);
    response.setToken(token);

    return response;
  }

  @Transactional(readOnly = true)
  public AuthResponseDto login(LoginRequestDto request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
    );
    String token = jwtTokenProvider.generateToken(authentication);

    log.info("User {} successfully logged in", request.getEmail());

    UserProfileDto profileDto = userService.getMyProfile(authentication);
    AuthResponseDto response = new AuthResponseDto();
    response.setUser(profileDto);
    response.setToken(token);

    return response;
  }

  private Authentication createAuthentication(User user) {
    var authorities = user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority(role.getName()))
        .toList();

    return new UsernamePasswordAuthenticationToken(
        user.getEmail(),
        user.getPasswordHash(),
        authorities
    );
  }
}
