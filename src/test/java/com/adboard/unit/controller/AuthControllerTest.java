package com.adboard.unit.controller;

import com.adboard.controller.AuthController;
import com.adboard.dto.request.auth.LoginRequestDto;
import com.adboard.dto.request.auth.RegisterRequestDto;
import com.adboard.dto.response.auth.AuthResponseDto;
import com.adboard.dto.response.user.UserProfileDto;
import com.adboard.exception.GlobalExceptionHandler;
import com.adboard.exception.UserAlreadyExistsException;
import com.adboard.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private AuthService authService;

  @InjectMocks
  private AuthController authController;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(authController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  /**
   * Testing login
   * */

  @Test
  @DisplayName("Should return 200 and token when login is successful")
  void login_success() throws Exception {
    LoginRequestDto request = new LoginRequestDto();
    request.setEmail("test@test.com");
    request.setPassword("password123");

    AuthResponseDto response = new AuthResponseDto();
    response.setToken("jwt");

    when(authService.login(any(LoginRequestDto.class))).thenReturn(response);

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt"));
  }

  @Test
  @DisplayName("Should return 401 when login credentials are invalid")
  void login_badCredentials_returns401() throws Exception {
    LoginRequestDto request = new LoginRequestDto();
    request.setEmail("test@test.com");
    request.setPassword("Неверный пароль");

    when(authService.login(any(LoginRequestDto.class)))
        .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid email or password"));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  /**
   * Testing register
   * */

  @Test
  @DisplayName("Should return 200 and token when registration is successful")
  void register_success() throws Exception {
    RegisterRequestDto request = new RegisterRequestDto();
    request.setUsername("Тестовый пользователь");
    request.setEmail("test@test.com");
    request.setPassword("password123");
    request.setPhone("+123456789");
    request.setCity("Москва");

    AuthResponseDto response = new AuthResponseDto();
    response.setToken("jwt");
    UserProfileDto profile = new UserProfileDto();
    profile.setEmail("test@test.com");
    response.setUser(profile);

    when(authService.register(any(RegisterRequestDto.class))).thenReturn(response);

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt"))
        .andExpect(jsonPath("$.user.email").value("test@test.com"));
  }

  @Test
  @DisplayName("Should return 400 when register request is invalid")
  void register_invalidRequest_returns400() throws Exception {
    RegisterRequestDto invalidRequest = new RegisterRequestDto();

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 409 when email is already registered")
  void register_emailExists_returns409() throws Exception {
    RegisterRequestDto request = new RegisterRequestDto();
    request.setUsername("Тестовый пользователь");
    request.setEmail("test@test.com");
    request.setPassword("password123");
    request.setPhone("+123456789");
    request.setCity("Москва");

    when(authService.register(any(RegisterRequestDto.class)))
        .thenThrow(new UserAlreadyExistsException("Email already registered: test@test.com"));

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email already registered: test@test.com"));
  }
}
