package com.adboard.controller;

import com.adboard.dto.request.ad.AdCreateRequestDto;
import com.adboard.dto.request.ad.AdUpdateRequestDto;
import com.adboard.dto.response.ad.AdResponseDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
@Tag(name = "Advertisements", description = "Ad management and search")
public class AdController {

  private final AdService adService;

  @Operation(summary = "Search and filter ads with pagination")
  @GetMapping
  public ResponseEntity<PageResponse<AdResponseDto>> searchAds(
      @Parameter(description = "Page number (starting from 0)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
      @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
      @Parameter(description = "Category ID") @RequestParam(required = false) Long categoryId,
      @Parameter(description = "Min price") @RequestParam(required = false) BigDecimal minPrice,
      @Parameter(description = "Max price") @RequestParam(required = false) BigDecimal maxPrice) {
    log.info("Search request: page={}, size={}, keyword={}, categoryId={}, minPrice={}, maxPrice={}",
        page, size, keyword, categoryId, minPrice, maxPrice);
    PageResponse<AdResponseDto> result = adService.searchAds(page, size, keyword, categoryId, minPrice, maxPrice);
    log.info("Search completed. Found {} ads on page {}", result.getContent().size(), page);
    return ResponseEntity.ok(result);
  }

  @Operation(summary = "Get ad by ID")
  @GetMapping("/{id}")
  public ResponseEntity<AdResponseDto> getAdById(@PathVariable Long id) {
    log.info("Fetching ad details: id={}", id);
    AdResponseDto ad = adService.getAdById(id);
    return ResponseEntity.ok(ad);
  }

  @Operation(summary = "Create a new ad")
  @PostMapping
  public ResponseEntity<AdResponseDto> createAd(
      @Valid @RequestBody AdCreateRequestDto request,
      Authentication authentication) {
    log.info("Create ad request by user: {}", authentication.getName());
    AdResponseDto createdAd = adService.createAd(request, authentication);
    log.info("Ad created successfully: id={}", createdAd.getId());
    return ResponseEntity.status(201).body(createdAd);
  }

  @Operation(summary = "Edit ad")
  @PutMapping("/{id}")
  public ResponseEntity<AdResponseDto> updateAd(
      @PathVariable Long id,
      @Valid @RequestBody AdUpdateRequestDto request,
      Authentication authentication) {
    log.info("Update ad request: id={}, user={}", id, authentication.getName());
    AdResponseDto updatedAd = adService.updateAd(id, request, authentication);
    log.info("Ad updated successfully: id={}", id);
    return ResponseEntity.ok(updatedAd);
  }

  @Operation(summary = "Delete ad")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAd(@PathVariable Long id, Authentication authentication) {
    log.info("Delete ad request: id={}, user={}", id, authentication.getName());
    adService.deleteAd(id, authentication);
    log.info("Ad deleted successfully: id={}", id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Activate paid promotion in the top")
  @PostMapping("/{id}/promote")
  public ResponseEntity<AdResponseDto> promoteAd(@PathVariable Long id, Authentication authentication) {
    log.info("Promote ad request: id={}, user={}", id, authentication.getName());
    AdResponseDto promotedAd = adService.promoteAd(id, authentication);
    log.info("Ad promoted successfully: id={}, expires={}", id, promotedAd.getPromoteExpiresAt());
    return ResponseEntity.ok(promotedAd);
  }
}
