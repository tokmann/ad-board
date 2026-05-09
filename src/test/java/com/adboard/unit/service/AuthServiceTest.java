package com.adboard.unit.service;

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
import com.adboard.service.AuthService;
import com.adboard.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private UserService userService;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private AuthenticationManager authenticationManager;

  @InjectMocks
  private AuthService authService;

  private RegisterRequestDto registerRequest;
  private LoginRequestDto loginRequest;
  private User mappedUser;
  private Role roleUser;
  private UserProfileDto userProfileDto;
  private Authentication auth;

  @BeforeEach
  void setUp() {
    roleUser = new Role();
    roleUser.setId(1L);
    roleUser.setName("ROLE_USER");

    registerRequest = new RegisterRequestDto();
    registerRequest.setEmail("test@mail.com");
    registerRequest.setUsername("testuser");
    registerRequest.setPassword("plainPass");

    loginRequest = new LoginRequestDto();
    loginRequest.setEmail("test@mail.com");
    loginRequest.setPassword("plainPass");

    mappedUser = new User();
    mappedUser.setEmail("test@mail.com");
    mappedUser.setUsername("testuser");

    userProfileDto = new UserProfileDto();
    userProfileDto.setEmail("test@mail.com");

    auth = new UsernamePasswordAuthenticationToken(
        "test@mail.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
    );
  }

  /**
   * Testing register
   * */

  @Test
  @DisplayName("Should create user with ROLE_USER for valid registration request")
  void register_validRequest_createsUser() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));
    when(userMapper.toEntity(registerRequest)).thenReturn(mappedUser);
    when(passwordEncoder.encode("plainPass")).thenReturn("hashed");
    when(userMapper.toProfileDto(any(User.class))).thenReturn(userProfileDto);
    when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt");

    AuthResponseDto result = authService.register(registerRequest);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User saved = userCaptor.getValue();

    assertThat(saved.getEmail()).isEqualTo("test@mail.com");
    assertThat(saved.getPasswordHash()).isEqualTo("hashed");
    assertThat(saved.getRoles()).containsExactly(roleUser);
    assertThat(result.getToken()).isEqualTo("jwt");
  }

  @Test
  @DisplayName("Should throw exception when email is already registered")
  void register_throws_whenEmailExists() {
    when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(new User()));

    assertThatThrownBy(() -> authService.register(registerRequest))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessageContaining("Email already registered");

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when username is already in use")
  void register_throws_whenUsernameExists() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(new User()));

    assertThatThrownBy(() -> authService.register(registerRequest))
        .isInstanceOf(UserAlreadyExistsException.class);

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should encode password before saving user")
  void register_encodesPassword() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));
    when(userMapper.toEntity(registerRequest)).thenReturn(mappedUser);
    when(passwordEncoder.encode("plainPass")).thenReturn("hashed");
    when(userMapper.toProfileDto(any(User.class))).thenReturn(userProfileDto);
    when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt");

    authService.register(registerRequest);

    verify(passwordEncoder).encode("plainPass");
  }

  @Test
  @DisplayName("Should assign ROLE_USER to new registered user")
  void register_assignsRoleUser() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));
    when(userMapper.toEntity(registerRequest)).thenReturn(mappedUser);
    when(passwordEncoder.encode(anyString())).thenReturn("hashed");
    when(userMapper.toProfileDto(any(User.class))).thenReturn(userProfileDto);
    when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt");

    authService.register(registerRequest);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertThat(userCaptor.getValue().getRoles()).containsExactly(roleUser);
  }

  /**
   * Testing login
   * */

  @Test
  @DisplayName("Should return token and profile for valid credentials")
  void login_validCredentials_returnsToken() {
    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
    when(jwtTokenProvider.generateToken(auth)).thenReturn("jwt");
    when(userService.getMyProfile(auth)).thenReturn(userProfileDto);

    AuthResponseDto result = authService.login(loginRequest);

    assertThat(result.getToken()).isEqualTo("jwt");
    verify(userService).getMyProfile(auth);
  }

  @Test
  @DisplayName("Should throw exception when password is incorrect")
  void login_wrongPassword_throws() {
    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  @DisplayName("Should throw exception when user does not exist")
  void login_nonExistentUser_throws() {
    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenThrow(new UsernameNotFoundException("User not found"));

    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}