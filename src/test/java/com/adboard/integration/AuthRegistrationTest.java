package com.adboard.integration;

import com.adboard.dto.request.auth.RegisterRequestDto;
import com.adboard.entity.User;
import com.adboard.entity.reference.Role;
import com.adboard.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("User registration scenarios")
class AuthRegistrationTest extends IntegrationTestBase {

  @Autowired
  private UserRepository userRepository;

  @Value("${app.security.admin-secret}")
  private String adminSecret;

  @Test
  @DisplayName("Should register regular user with ROLE_USER and return JWT token")
  void registerUser_success_returnsTokenAndRoleUser() throws Exception {
    RegisterRequestDto request = new RegisterRequestDto();
    request.setUsername("Тестовый пользователь");
    request.setEmail("user@test.com");
    request.setPassword("password123");
    request.setPhone("+79990001122");
    request.setCity("Москва");

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists())
        .andExpect(content().json("""
                {
                  "user": {
                    "username": "Тестовый пользователь",
                    "email": "user@test.com",
                    "phone": "+79990001122",
                    "city": "Москва",
                    "roles": ["ROLE_USER"]
                  }
                }
                """, false));

    User saved = userRepository.findByEmail("user@test.com").orElseThrow();
    assertThat(saved.getRoles()).extracting(Role::getName).contains("ROLE_USER");
    assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
  }

  @Test
  @DisplayName("Should register admin with ROLE_ADMIN when valid secret provided")
  void registerAdmin_withValidSecret_success_returnsTokenAndRoleAdmin() throws Exception {
    RegisterRequestDto request = new RegisterRequestDto();
    request.setUsername("Тестовый админ");
    request.setEmail("admin@test.com");
    request.setPassword("password123");
    request.setPhone("+79990001122");
    request.setCity("Москва");
    request.setAdminSecretToken(adminSecret);

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists())
        .andExpect(content().json("""
                {
                  "user": {
                    "username": "Тестовый админ",
                    "email": "admin@test.com",
                    "phone": "+79990001122",
                    "city": "Москва",
                    "roles": ["ROLE_USER","ROLE_ADMIN"]
                  }
                }
                """, false));

    User saved = userRepository.findByEmail("admin@test.com").orElseThrow();
    assertThat(saved.getRoles()).extracting(Role::getName).contains("ROLE_ADMIN");
    assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
  }

  @Test
  @DisplayName("Should reject admin registration with invalid secret token")
  void registerAdmin_withInvalidSecret_returnsBadRequest() throws Exception {
    RegisterRequestDto request = new RegisterRequestDto();
    request.setUsername("Фейк админ");
    request.setEmail("fake@test.com");
    request.setPassword("password123");
    request.setPhone("+1234567890");
    request.setCity("Москва");
    request.setAdminSecretToken("WRONG_SECRET");

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    assertThat(userRepository.findByEmail("fake@test.com")).isEmpty();
  }

  @Test
  @DisplayName("Should reject registration when email already exists")
  void registerUser_withExistingEmail_returnsConflict() throws Exception {
    RegisterRequestDto first = new RegisterRequestDto();
    first.setUsername("Первый пользователь");
    first.setEmail("user@test.com");
    first.setPassword("password123");
    first.setPhone("+1234567890");
    first.setCity("Москва");

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(first)))
        .andExpect(status().isOk());

    RegisterRequestDto second = new RegisterRequestDto();
    second.setUsername("Второй пользователь");
    second.setEmail("user@test.com");
    second.setPassword("password123");
    second.setPhone("+1234567899");
    second.setCity("Москва");

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(second)))
        .andExpect(status().isConflict());
  }
}