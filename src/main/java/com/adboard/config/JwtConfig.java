package com.adboard.config;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
@PropertySource("classpath:application.properties")
public class JwtConfig {

  private final String secret;

  @Getter
  private final Duration expiration;

  @Getter
  private final Duration refreshExpiration;

  public JwtConfig(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration}") long expirationHours,
      @Value("${jwt.refreshExpiration}") long refreshHours
  ) {
    this.secret = secret;
    this.expiration = Duration.ofHours(expirationHours);
    this.refreshExpiration = Duration.ofHours(refreshHours);
  }

  public SecretKey getSecretKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

}