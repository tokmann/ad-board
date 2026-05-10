package com.adboard.unit.service;

import com.adboard.dto.mapper.ReviewMapper;
import com.adboard.dto.request.review.ReviewRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.review.ReviewResponseDto;
import com.adboard.entity.Review;
import com.adboard.entity.User;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.exception.ReviewAlreadyExistsException;
import com.adboard.repository.ReviewRepository;
import com.adboard.repository.UserRepository;
import com.adboard.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ReviewMapper reviewMapper;

  @InjectMocks
  private ReviewService reviewService;

  private User reviewer;
  private User seller;
  private Review review;
  private ReviewResponseDto reviewDto;
  private ReviewRequestDto request;
  private Authentication auth;

  @BeforeEach
  void setUp() {
    reviewer = new User();
    reviewer.setId(10L);
    reviewer.setEmail("reviewer@test.com");

    seller = new User();
    seller.setId(1L);
    seller.setEmail("seller@test.com");

    review = new Review();
    review.setId(100L);
    review.setRating(5);
    review.setCommentText("Отлично!");
    review.setReviewer(reviewer);
    review.setSeller(seller);

    reviewDto = new ReviewResponseDto();
    reviewDto.setId(100L);
    reviewDto.setRating(5);
    reviewDto.setCommentText("Отлично!");

    request = new ReviewRequestDto();
    request.setRating(5);
    request.setCommentText("Отлично!");

    auth = mock(Authentication.class);
  }

  /**
   * Testing addReview
   * */

  @Test
  @DisplayName("Should create review with correct reviewer and seller for valid request")
  void addReview_validRequest_createsReview() {
    when(auth.getName()).thenReturn("reviewer@test.com");
    when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
    when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
    when(reviewRepository.findByReviewerIdAndSellerId(10L, 1L)).thenReturn(Optional.empty());
    when(reviewMapper.toEntity(request)).thenAnswer(inv -> {
      Review newReview = new Review();
      newReview.setRating(request.getRating());
      newReview.setCommentText(request.getCommentText());
      return newReview;
    });
    when(reviewMapper.toResponseDto(any(Review.class))).thenReturn(reviewDto);

    reviewService.addReview(1L, request, auth);

    ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
    verify(reviewRepository).save(captor.capture());
    Review saved = captor.getValue();

    assertThat(saved.getReviewer()).isEqualTo(reviewer);
    assertThat(saved.getSeller()).isEqualTo(seller);
    assertThat(saved.getRating()).isEqualTo(5);
  }

  @Test
  @DisplayName("Should throw exception when reviewer is not found")
  void addReview_throws_whenReviewerNotFound() {
    when(auth.getName()).thenReturn("reviewer@test.com");
    when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.addReview(1L, request, auth))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("Should throw exception when seller is not found")
  void addReview_throws_whenSellerNotFound() {
    when(auth.getName()).thenReturn("reviewer@test.com");
    when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.addReview(1L, request, auth))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("Should throw exception when user tries to review themselves")
  void addReview_throws_whenReviewingSelf() {
    seller.setId(10L);

    when(auth.getName()).thenReturn("reviewer@test.com");
    when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
    when(userRepository.findById(10L)).thenReturn(Optional.of(seller));

    assertThatThrownBy(() -> reviewService.addReview(10L, request, auth))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should throw exception when user has already reviewed this seller")
  void addReview_throws_whenReviewAlreadyExists() {
    when(auth.getName()).thenReturn("reviewer@test.com");
    when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
    when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
    when(reviewRepository.findByReviewerIdAndSellerId(10L, 1L)).thenReturn(Optional.of(review));

    assertThatThrownBy(() -> reviewService.addReview(1L, request, auth))
        .isInstanceOf(ReviewAlreadyExistsException.class);
  }

  /**
   * Testing getReviewsBySellerId
   * */

  @Test
  @DisplayName("Should return paginated reviews for existing seller")
  void getReviewsBySellerId_returnsPaginatedReviews_whenSellerExists() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
    when(reviewRepository.countBySellerId(1L)).thenReturn(12L);
    when(reviewRepository.findBySellerId(1L, 0, 10)).thenReturn(List.of(review));
    when(reviewMapper.toResponseDto(review)).thenReturn(reviewDto);

    PageResponse<ReviewResponseDto> result = reviewService.getReviewsBySellerId(1L, 0, 10);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(12);
    assertThat(result.getTotalPages()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should return empty page when seller has no reviews")
  void getReviewsBySellerId_returnsEmptyPage_whenNoReviews() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
    when(reviewRepository.countBySellerId(1L)).thenReturn(0L);
    when(reviewRepository.findBySellerId(1L, 0, 10)).thenReturn(List.of());

    PageResponse<ReviewResponseDto> result = reviewService.getReviewsBySellerId(1L, 0, 10);

    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isEqualTo(0);
    assertThat(result.getTotalPages()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should throw exception when seller is not found")
  void getReviewsBySellerId_throws_whenSellerNotFound() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.getReviewsBySellerId(999L, 0, 10))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}