package com.adboard.unit.controller;

import com.adboard.controller.UserController;
import com.adboard.dto.request.user.ProfileUpdateRequestDto;
import com.adboard.dto.response.user.UserProfileDto;
import com.adboard.exception.GlobalExceptionHandler;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock
  private UserService userService;

  @Mock
  private Authentication auth;

  @InjectMocks
  private UserController userController;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    when(auth.getName()).thenReturn("test@test.com");

    HandlerMethodArgumentResolver authResolver = new HandlerMethodArgumentResolver() {

      @Override
      public boolean supportsParameter(MethodParameter parameter) {
        return Authentication.class.isAssignableFrom(parameter.getParameterType());
      }

      @Override
      public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return auth;
      }
    };

    mockMvc = MockMvcBuilders.standaloneSetup(userController)
        .setCustomArgumentResolvers(authResolver)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  /**
   * Testing getMyProfile
   */
  @Test
  @DisplayName("Should return 200 and profile data")
  void getMyProfile_success() throws Exception {
    UserProfileDto profile = new UserProfileDto();
    profile.setEmail("test@test.com");
    profile.setUsername("testuser");

    when(userService.getMyProfile(any())).thenReturn(profile);

    mockMvc.perform(get("/api/users/me")
            .principal(auth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@test.com"))
        .andExpect(jsonPath("$.username").value("testuser"));
  }

  @Test
  @DisplayName("Should return 404 when user not found")
  void getMyProfile_notFound() throws Exception {
    when(userService.getMyProfile(any()))
        .thenThrow(new ResourceNotFoundException("User not found"));

    mockMvc.perform(get("/api/users/me")
            .principal(auth))
        .andExpect(status().isNotFound());
  }

  /**
   * Testing updateMyProfile
   */
  @Test
  @DisplayName("Should return 200 when profile is updated")
  void updateMyProfile_success() throws Exception {
    ProfileUpdateRequestDto request = new ProfileUpdateRequestDto();
    request.setUsername("New username");
    request.setCity("Old city");
    request.setPhone("Old phone");

    UserProfileDto response = new UserProfileDto();
    response.setUsername("New username");

    when(userService.updateMyProfile(any(), any())).thenReturn(response);

    mockMvc.perform(put("/api/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("New username"));
  }

  @Test
  @DisplayName("Should return 400 when profile update request is invalid")
  void updateMyProfile_invalidDto_returns400() throws Exception {
    ProfileUpdateRequestDto invalidRequest = new ProfileUpdateRequestDto();
    invalidRequest.setUsername("");

    mockMvc.perform(put("/api/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
            .principal(auth))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 404 when updating non-existent user profile")
  void updateMyProfile_userNotFound() throws Exception {
    ProfileUpdateRequestDto request = new ProfileUpdateRequestDto();
    request.setUsername("New username");
    request.setCity("Old city");
    request.setPhone("Old phone");

    when(userService.updateMyProfile(any(), any()))
        .thenThrow(new ResourceNotFoundException("User not found"));

    mockMvc.perform(put("/api/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isNotFound());
  }
}