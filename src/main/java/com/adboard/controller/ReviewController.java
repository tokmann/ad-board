package com.adboard.controller;

import com.adboard.dto.request.ReviewRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.ReviewResponseDto;
import com.adboard.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users/{sellerId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Seller ratings and reviews")
public class ReviewController {

  private final ReviewService reviewService;

  @Operation(summary = "Leave comment for the seller")
  @PostMapping
  public ResponseEntity<ReviewResponseDto> addReview(
      @Parameter(description = "Seller ID") @PathVariable Long sellerId,
      @Valid @RequestBody ReviewRequestDto request,
      Authentication authentication) {
    log.info("Adding review for seller: {} by user: {}", sellerId, authentication.getName());
    ReviewResponseDto created = reviewService.addReview(sellerId, request, authentication);
    log.info("Review added successfully: id={}", created.getId());
    return ResponseEntity.status(201).body(created);
  }

  @Operation(summary = "Get comments about the seller")
  @GetMapping
  public ResponseEntity<PageResponse<ReviewResponseDto>> getReviews(
      @Parameter(description = "Seller ID") @PathVariable Long sellerId,
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
    log.info("Fetching reviews for seller: {}, page={}, size={}", sellerId, page, size);
    PageResponse<ReviewResponseDto> result = reviewService.getReviewsBySellerId(sellerId, page, size);
    log.info("Fetched {} reviews for seller {}", result.getContent().size(), sellerId);
    return ResponseEntity.ok(result);
  }
}
