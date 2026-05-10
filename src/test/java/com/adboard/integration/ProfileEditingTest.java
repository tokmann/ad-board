package com.adboard.integration;

import com.adboard.dto.request.user.ProfileUpdateRequestDto;
import com.adboard.entity.User;
import com.adboard.entity.reference.Role;
import com.adboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("User profile management scenarios")
class ProfileEditingTest extends IntegrationTestBase {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User testUser;
  private String validToken;

  @BeforeEach
  void setUp() throws Exception {
    testUser = new User();
    testUser.setUsername("Тестовый пользователь");
    testUser.setEmail("profile@test.com");
    testUser.setPasswordHash(passwordEncoder.encode("password123"));
    testUser.setPhone("+79990001111");
    testUser.setCity("Москва");
    Set<Role> roles = new HashSet<>();
    roles.add(new Role(1L, "ROLE_USER"));
    testUser.setRoles(roles);
    userRepository.save(testUser);

    String loginRequest = """
            {
              "email": "profile@test.com",
              "password": "password123"
            }
            """;

    validToken = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequest))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
        .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
  }

  @Test
  @DisplayName("Should return user profile when authenticated")
  void getMyProfile_authenticated_returnsUserProfile() throws Exception {
    mockMvc.perform(get("/api/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + validToken))
        .andExpect(status().isOk())
        .andExpect(content().json("""
                {
                  "username": "Тестовый пользователь",
                  "email": "profile@test.com",
                  "phone": "+79990001111",
                  "city": "Москва",
                  "roles": ["ROLE_USER"]
                }
                """, false));
  }

  @Test
  @DisplayName("Should update profile fields and return updated DTO")
  void updateMyProfile_validRequest_updatesFieldsAndReturnsDto() throws Exception {
    ProfileUpdateRequestDto request = new ProfileUpdateRequestDto();
    request.setUsername("Обновленный пользователь");
    request.setPhone("+79998887766");
    request.setCity("Новый город");

    mockMvc.perform(put("/api/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + validToken)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().json("""
                {
                  "username": "Обновленный пользователь",
                  "email": "profile@test.com",
                  "phone": "+79998887766",
                  "city": "Новый город",
                  "roles": ["ROLE_USER"]
                }
                """, false));

    User saved = userRepository.findByEmail("profile@test.com").orElseThrow();
    assertThat(saved.getUsername()).isEqualTo("Обновленный пользователь");
    assertThat(saved.getPhone()).isEqualTo("+79998887766");
    assertThat(saved.getCity()).isEqualTo("Новый город");
    assertThat(saved.getEmail()).isEqualTo("profile@test.com");
    assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
  }

  @Test
  @DisplayName("Should reject update with invalid request data")
  void updateMyProfile_invalidRequest_returnsBadRequest() throws Exception {
    ProfileUpdateRequestDto request = new ProfileUpdateRequestDto();
    request.setUsername("");
    request.setPhone("+79998887766");
    request.setCity("Москва");

    mockMvc.perform(put("/api/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + validToken)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());

    User saved = userRepository.findByEmail("profile@test.com").orElseThrow();
    assertThat(saved.getUsername()).isEqualTo("Тестовый пользователь");
    assertThat(saved.getPhone()).isEqualTo("+79990001111");
  }

  @Test
  @DisplayName("Should reject profile access without authentication")
  void getMyProfile_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/users/me")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}