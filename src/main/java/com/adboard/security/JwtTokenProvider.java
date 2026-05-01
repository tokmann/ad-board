package com.adboard.security;

import com.adboard.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final JwtConfig jwtConfig;

  public String generateToken(Authentication authentication) {
    return generateToken(authentication.getName());
  }

  public String generateToken(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration().toMillis());
    return Jwts.builder()
        .subject(username)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(jwtConfig.getSecretKey())
        .compact();
  }

  public String getUsernameFromToken(String token) {
    return getClaimFromToken(token, Claims::getSubject);
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(jwtConfig.getSecretKey()).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("Invalid JWT token: {}", e.getMessage());
      return false;
    }
  }

  private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = Jwts.parser()
        .verifyWith(jwtConfig.getSecretKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
    return claimsResolver.apply(claims);
  }
}
