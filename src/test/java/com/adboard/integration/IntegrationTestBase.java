package com.adboard.integration;

import com.adboard.config.AppConfig;
import com.adboard.config.JpaConfig;
import com.adboard.config.JwtConfig;
import com.adboard.config.LiquibaseConfig;
import com.adboard.config.SecurityConfig;
import com.adboard.config.WebMvcConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    JpaConfig.class,
    AppConfig.class,
    WebMvcConfig.class,
    SecurityConfig.class,
    JwtConfig.class,
    LiquibaseConfig.class
})
@WebAppConfiguration
@Transactional
@RequiredArgsConstructor
public abstract class IntegrationTestBase {

  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  static {
    postgres.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("jdbc.url", postgres::getJdbcUrl);
    registry.add("jdbc.username", postgres::getUsername);
    registry.add("jdbc.password", postgres::getPassword);
  }

  @Autowired
  protected WebApplicationContext wac;

  @Autowired
  protected ObjectMapper objectMapper;

  protected MockMvc mockMvc;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
        .apply(SecurityMockMvcConfigurers.springSecurity())
        .build();
  }
}