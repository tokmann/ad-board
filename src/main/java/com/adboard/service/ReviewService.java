package com.adboard.service;

import com.adboard.dto.request.ReviewRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.ReviewResponseDto;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

  public ReviewResponseDto addReview(Long sellerId, ReviewRequestDto request, Authentication authentication) {
    return null;
  }

  public PageResponse<ReviewResponseDto> getReviewsBySellerId(Long sellerId, int page, int size) {
    return null;
  }
}
