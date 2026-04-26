package com.adboard.controller;

import com.adboard.dto.response.ad.AdResponseDto;
import com.adboard.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ads/{adId}/promote")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Paid promotion of ads at the top of search results")
public class PromotionController {

  private final AdService adService;

  @Operation(summary = "Activate paid ad promotion")
  @PostMapping
  public ResponseEntity<AdResponseDto> promoteAd(
      @Parameter(description = "Ad ID") @PathVariable Long adId,
      Authentication authentication) {
    log.info("Promoting ad: {} by user: {}", adId, authentication.getName());
    AdResponseDto promoted = adService.promoteAd(adId, authentication);
    log.info("Ad promoted successfully: id={}, expires={}", adId, promoted.getPromoteExpiresAt());
    return ResponseEntity.ok(promoted);
  }
}
