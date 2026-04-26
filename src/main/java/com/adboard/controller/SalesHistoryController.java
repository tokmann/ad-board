package com.adboard.controller;

import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.ad.AdResponseDto;
import com.adboard.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users/me/sales")
@RequiredArgsConstructor
@Tag(name = "Sales History", description = "User's history of sold ads")
public class SalesHistoryController {

  private final AdService adService;

  @Operation(summary = "Get the sales history of the current user")
  @GetMapping
  public ResponseEntity<PageResponse<AdResponseDto>> getSalesHistory(
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
      Authentication authentication) {
    log.info("Fetching sales history for user: {}, page={}, size={}", authentication.getName(), page, size);
    PageResponse<AdResponseDto> result = adService.getSalesHistory(page, size, authentication);
    log.info("Fetched {} sold ads for user {}", result.getContent().size(), authentication.getName());
    return ResponseEntity.ok(result);
  }
}
