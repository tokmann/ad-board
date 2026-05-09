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
import org.springframework.security.access.prepost.PreAuthorize;
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
      @Parameter(description = "Page number (starting from 0)") @RequestParam(name = "page", defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(name = "size", defaultValue = "10") int size,
      @Parameter(description = "Search keyword") @RequestParam(name = "keyword", required = false) String keyword,
      @Parameter(description = "Category ID") @RequestParam(name = "categoryId", required = false) Long categoryId,
      @Parameter(description = "Min price") @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
      @Parameter(description = "Max price") @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice) {
    log.debug("REST request to search ads: page={}, size={}, keyword={}, categoryId={}", page, size, keyword, categoryId);
    PageResponse<AdResponseDto> result = adService.searchAds(page, size, keyword, categoryId, minPrice, maxPrice);
    return ResponseEntity.ok(result);
  }

  @Operation(summary = "Get ad by ID")
  @GetMapping("{id}")
  public ResponseEntity<AdResponseDto> getAdById(@PathVariable("id") Long id) {
    log.debug("REST request to get ad by id: {}", id);
    AdResponseDto ad = adService.getAdById(id);
    return ResponseEntity.ok(ad);
  }

  @Operation(summary = "Create a new ad")
  @PostMapping
  public ResponseEntity<AdResponseDto> createAd(
      @Valid @RequestBody AdCreateRequestDto request,
      Authentication authentication) {
    log.debug("REST request to create ad by user: {}", authentication.getName());
    AdResponseDto createdAd = adService.createAd(request, authentication);
    return ResponseEntity.status(201).body(createdAd);
  }

  @Operation(summary = "Edit ad")
  @PutMapping("/{id}")
  public ResponseEntity<AdResponseDto> updateAd(
      @PathVariable("id") Long id,
      @Valid @RequestBody AdUpdateRequestDto request,
      Authentication authentication) {
    log.debug("REST request to update ad: id={}, user={}", id, authentication.getName());
    AdResponseDto updatedAd = adService.updateAd(id, request, authentication);
    return ResponseEntity.ok(updatedAd);
  }

  @Operation(summary = "Delete ad")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAd(@PathVariable("id") Long id, Authentication authentication) {
    log.debug("REST request to delete ad: id={}, user={}", id, authentication.getName());
    adService.deleteAd(id, authentication);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Publish ad after moderation (Admin only)")
  @PutMapping("/{id}/publish")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AdResponseDto> publishAd(
      @Parameter(description = "Ad ID") @PathVariable("id") Long id,
      Authentication authentication) {

    log.debug("REST request to publish ad: {} by admin: {}", id, authentication.getName());
    AdResponseDto published = adService.publishAd(id, authentication);
    return ResponseEntity.ok(published);
  }
}
