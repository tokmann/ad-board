package com.adboard.service;

import com.adboard.dto.mapper.ReviewMapper;
import com.adboard.dto.request.review.ReviewRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.review.ReviewResponseDto;
import com.adboard.entity.Review;
import com.adboard.entity.User;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.exception.ReviewAlreadyExistsException;
import com.adboard.exception.UnauthorizedActionException;
import com.adboard.repository.ReviewRepository;
import com.adboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;
  private final ReviewMapper reviewMapper;

  @Transactional
  public ReviewResponseDto addReview(Long sellerId, ReviewRequestDto request, Authentication authentication) {
    String reviewerEmail = authentication.getName();

    User reviewer = userRepository.findByEmail(reviewerEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + reviewerEmail));

    User seller = userRepository.findById(sellerId)
        .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

    if (reviewer.getId().equals(seller.getId())) {
      throw new IllegalArgumentException("Cannot leave a review for yourself");
    }

    if (reviewRepository.findByReviewerIdAndSellerId(reviewer.getId(), seller.getId()).isPresent()) {
      throw new ReviewAlreadyExistsException("You have already reviewed this seller");
    }

    Review review = reviewMapper.toEntity(request);
    review.setReviewer(reviewer);
    review.setSeller(seller);

    reviewRepository.save(review);

    log.info("Review added successfully: sellerId={}, reviewer={}, rating={}",
        sellerId, reviewerEmail, request.getRating());

    return reviewMapper.toResponseDto(review);
  }

  @Transactional(readOnly = true)
  public PageResponse<ReviewResponseDto> getReviewsBySellerId(Long sellerId, int page, int size) {
    if (userRepository.findById(sellerId).isEmpty()) {
      throw new ResourceNotFoundException("Seller not found with id: " + sellerId);
    }

    long totalElements = reviewRepository.countBySellerId(sellerId);
    List<Review> reviews = reviewRepository.findBySellerId(sellerId, page, size);

    List<ReviewResponseDto> content = reviews.stream()
        .map(reviewMapper::toResponseDto)
        .toList();

    int totalPages = (int) Math.ceil((double) totalElements / size);

    log.info("Successfully retrieved {} reviews for seller {} (page {}/{})",
        content.size(), sellerId, page + 1, totalPages);

    return new PageResponse<>(content, page, size, totalElements, totalPages);
  }
}
