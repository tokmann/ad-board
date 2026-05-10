package com.adboard.controller;

import com.adboard.dto.request.review.ReviewRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.review.ReviewResponseDto;
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
      @Parameter(description = "Seller ID") @PathVariable("sellerId") Long sellerId,
      @Valid @RequestBody ReviewRequestDto request,
      Authentication authentication) {
    log.info("REST request to add review for seller: {} by user: {}", sellerId, authentication.getName());
    ReviewResponseDto created = reviewService.addReview(sellerId, request, authentication);
    return ResponseEntity.status(201).body(created);
  }

  @Operation(summary = "Get comments about the seller")
  @GetMapping
  public ResponseEntity<PageResponse<ReviewResponseDto>> getReviews(
      @Parameter(description = "Seller ID") @PathVariable("sellerId") Long sellerId,
      @Parameter(description = "Page number") @RequestParam(name = "page", defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(name = "size", defaultValue = "10") int size) {
    log.info("REST request to fetch reviews for seller: {}, page={}", sellerId, page);
    PageResponse<ReviewResponseDto> result = reviewService.getReviewsBySellerId(sellerId, page, size);
    return ResponseEntity.ok(result);
  }
}
