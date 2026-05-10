package com.adboard.unit.controller;

import com.adboard.controller.SalesHistoryController;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.ad.AdResponseDto;
import com.adboard.exception.GlobalExceptionHandler;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
class SalesHistoryControllerTest {

  @Mock
  private AdService adService;

  @Mock
  private Authentication auth;

  @InjectMocks
  private SalesHistoryController salesHistoryController;

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

    mockMvc = MockMvcBuilders.standaloneSetup(salesHistoryController)
        .setCustomArgumentResolvers(authResolver)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  /**
   * Testing getSalesHistory
   * */

  @Test
  @DisplayName("Should return 200 and sales history page")
  void getSalesHistory_success() throws Exception {
    AdResponseDto adDto = new AdResponseDto();
    adDto.setId(1L);
    adDto.setTitle("Проданный iPhone");

    PageResponse<AdResponseDto> response = new PageResponse<>(
        List.of(adDto), 0, 10, 1, 1
    );

    when(adService.getSalesHistory(eq(0), eq(10), any())).thenReturn(response);

    mockMvc.perform(get("/api/users/me/sales")
            .param("page", "0")
            .param("size", "10")
            .principal(auth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Проданный iPhone"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("Should return 400 when page parameters are invalid")
  void getSalesHistory_invalidParams_returns400() throws Exception {
    mockMvc.perform(get("/api/users/me/sales")
            .param("page", "fdsgsa")
            .principal(auth))
        .andExpect(status().isBadRequest());
  }
}