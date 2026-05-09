package com.adboard.unit.controller;

import com.adboard.controller.PromotionController;
import com.adboard.dto.response.ad.AdResponseDto;
import com.adboard.exception.AdAlreadyPromotedException;
import com.adboard.exception.GlobalExceptionHandler;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.exception.UnauthorizedActionException;
import com.adboard.service.AdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PromotionControllerTest {

  @Mock
  private AdService adService;

  @Mock
  private Authentication auth;

  @InjectMocks
  private PromotionController promotionController;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    when(auth.getName()).thenReturn("test@test.com");

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

    mockMvc = MockMvcBuilders.standaloneSetup(promotionController)
        .setCustomArgumentResolvers(authResolver)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  /**
   * Testing promoteAd
   * */

  @Test
  @DisplayName("Should return 200 when ad is promoted successfully")
  void promoteAd_success() throws Exception {
    AdResponseDto response = new AdResponseDto();
    response.setId(1L);

    when(adService.promoteAd(eq(1L), any())).thenReturn(response);

    mockMvc.perform(post("/api/ads/1/promote")
            .principal(auth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @Test
  @DisplayName("Should return 404 when ad does not exist")
  void promoteAd_notFound() throws Exception {
    when(adService.promoteAd(eq(999L), any()))
        .thenThrow(new ResourceNotFoundException("Ad not found"));

    mockMvc.perform(post("/api/ads/999/promote")
            .principal(auth))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 403 when user is not the owner")
  void promoteAd_unauthorized() throws Exception {
    when(adService.promoteAd(eq(1L), any()))
        .thenThrow(new UnauthorizedActionException("You don't own this ad"));

    mockMvc.perform(post("/api/ads/1/promote")
            .principal(auth))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("You don't own this ad"));
  }

  @Test
  @DisplayName("Should return 409 when ad is already promoted")
  void promoteAd_alreadyPromoted() throws Exception {
    when(adService.promoteAd(eq(1L), any()))
        .thenThrow(new AdAlreadyPromotedException("Ad is already promoted"));

    mockMvc.perform(post("/api/ads/1/promote")
            .principal(auth))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Ad is already promoted"));
  }
}
