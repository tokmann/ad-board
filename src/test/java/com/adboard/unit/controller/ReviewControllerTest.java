package com.adboard.unit.controller;

import com.adboard.controller.ReviewController;
import com.adboard.dto.request.review.ReviewRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.review.ReviewResponseDto;
import com.adboard.exception.GlobalExceptionHandler;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.exception.ReviewAlreadyExistsException;
import com.adboard.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

  @Mock
  private ReviewService reviewService;

  @Mock
  private Authentication auth;

  @InjectMocks
  private ReviewController reviewController;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    HandlerMethodArgumentResolver authResolver = new HandlerMethodArgumentResolver() {

      @Override
      public boolean supportsParameter(MethodParameter parameter) {
        return Authentication.class.isAssignableFrom(parameter.getParameterType());
      }

      @Override
      public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return auth;
      }
    };

    mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
        .setCustomArgumentResolvers(authResolver)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  /**
   * Testing getReviews
   */

  @Test
  @DisplayName("Should return 200 and page of reviews")
  void getReviews_success() throws Exception {
    PageResponse<ReviewResponseDto> response = new PageResponse<>(List.of(), 0, 10, 0, 0);
    when(reviewService.getReviewsBySellerId(eq(1L), anyInt(), anyInt())).thenReturn(response);

    mockMvc.perform(get("/api/users/1/reviews")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk());
  }

  /**
   * Testing getReviews
   */

  @Test
  @DisplayName("Should return 404 when seller not found during fetch")
  void getReviews_sellerNotFound() throws Exception {
    when(reviewService.getReviewsBySellerId(eq(999L), anyInt(), anyInt()))
        .thenThrow(new ResourceNotFoundException("Seller not found"));

    mockMvc.perform(get("/api/users/999/reviews"))
        .andExpect(status().isNotFound());
  }

  /**
   * Testing getReviews
   */

  @Test
  @DisplayName("Should return 201 when review is added successfully")
  void addReview_success() throws Exception {
    when(auth.getName()).thenReturn("test@test.com");

    ReviewRequestDto request = new ReviewRequestDto();
    request.setRating(5);
    request.setCommentText("Отличный сервис!");

    ReviewResponseDto response = new ReviewResponseDto();
    response.setId(100L);

    when(reviewService.addReview(eq(1L), any(), any())).thenReturn(response);

    mockMvc.perform(post("/api/users/1/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(100L));
  }

  @Test
  @DisplayName("Should return 400 when reviewer tries to review himself")
  void addReview_selfReview_returns400() throws Exception {
    when(auth.getName()).thenReturn("test@test.com");

    ReviewRequestDto request = new ReviewRequestDto();
    request.setRating(5);
    request.setCommentText("Отличный сервис!");

    when(reviewService.addReview(eq(1L), any(), any()))
        .thenThrow(new IllegalArgumentException("Cannot leave a review for yourself"));

    mockMvc.perform(post("/api/users/1/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Cannot leave a review for yourself"));
  }

  @Test
  @DisplayName("Should return 409 when review already exists")
  void addReview_alreadyExists_returns409() throws Exception {
    when(auth.getName()).thenReturn("test@test.com");

    ReviewRequestDto request = new ReviewRequestDto();
    request.setRating(5);
    request.setCommentText("Отличный сервис!");

    when(reviewService.addReview(eq(1L), any(), any()))
        .thenThrow(new ReviewAlreadyExistsException("You have already reviewed this seller"));

    mockMvc.perform(post("/api/users/1/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("You have already reviewed this seller"));
  }

  @Test
  @DisplayName("Should return 400 when rating is missing")
  void addReview_invalidDto_returns400() throws Exception {
    when(auth.getName()).thenReturn("test@test.com");

    ReviewRequestDto invalidRequest = new ReviewRequestDto();
    invalidRequest.setCommentText("Хороший продавец");

    mockMvc.perform(post("/api/users/1/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
            .principal(auth))
        .andExpect(status().isBadRequest());
  }
}
