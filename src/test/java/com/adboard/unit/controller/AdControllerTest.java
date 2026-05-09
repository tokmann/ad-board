package com.adboard.unit.controller;

import com.adboard.controller.AdController;
import com.adboard.dto.request.ad.AdCreateRequestDto;
import com.adboard.dto.request.ad.AdUpdateRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.ad.AdResponseDto;
import com.adboard.exception.GlobalExceptionHandler;
import com.adboard.exception.InvalidAdStatusException;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.exception.UnauthorizedActionException;
import com.adboard.service.AdService;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class AdControllerTest {

  @Mock
  private AdService adService;

  @Mock
  private Authentication auth;

  @InjectMocks
  private AdController adController;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private MockMvc mockMvc;

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

    mockMvc = MockMvcBuilders.standaloneSetup(adController)
        .setCustomArgumentResolvers(authResolver)
        .setControllerAdvice(new GlobalExceptionHandler())
        .setMessageConverters(new MappingJackson2HttpMessageConverter())
        .build();
  }

  /**
   * Testing searchAds
   * */

  @Test
  @DisplayName("Should search ads with filters and return page")
  void searchAds_withFilters_success() throws Exception {
    when(auth.getName()).thenReturn("test@test.com");

    AdResponseDto adDto = new AdResponseDto();
    adDto.setId(1L);
    adDto.setTitle("iPhone 15");
    adDto.setPrice(new BigDecimal("1000.00"));

    PageResponse<AdResponseDto> response = new PageResponse<>(
        List.of(adDto), 0, 10, 1, 1
    );

    when(adService.searchAds(
        eq(0),
        eq(10),
        eq("iPhone"),
        eq(5L),
        eq(new BigDecimal("500")),
        eq(new BigDecimal("1500"))
    )).thenReturn(response);

    mockMvc.perform(get("/api/ads")
            .param("page", "0")
            .param("size", "10")
            .param("keyword", "iPhone")
            .param("categoryId", "5")
            .param("minPrice", "500")
            .param("maxPrice", "1500")
            .principal(auth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("iPhone 15"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  /**
   * Testing getAdById
   */
  @Test
  @DisplayName("Should return 200 when ad is found by ID")
  void getAdById_success() throws Exception {
    AdResponseDto response = new AdResponseDto();
    response.setId(1L);
    response.setTitle("iPhone 15");

    when(adService.getAdById(1L)).thenReturn(response);

    mockMvc.perform(get("/api/ads/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.title").value("iPhone 15"));
  }

  @Test
  @DisplayName("Should return 404 when ad not found by ID")
  void getAdById_notFound() throws Exception {
    when(adService.getAdById(999L))
        .thenThrow(new ResourceNotFoundException("Ad not found"));

    mockMvc.perform(get("/api/ads/999"))
        .andExpect(status().isNotFound());
  }

  /**
   * Testing createAd
   * */

  @Test
  @DisplayName("Should return 201 when ad is created successfully")
  void createAd_success() throws Exception {
    when(auth.getName()).thenReturn("test@test.com");

    AdCreateRequestDto request = new AdCreateRequestDto();
    request.setTitle("Test ad");
    request.setPrice(new BigDecimal("100.00"));
    request.setDescription("Some description");
    request.setCategoryId(1L);

    AdResponseDto response = new AdResponseDto();
    response.setId(1L);
    response.setTitle("Test ad");

    when(adService.createAd(any(), any())).thenReturn(response);

    mockMvc.perform(post("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.title").value("Test ad"));
  }

  @Test
  @DisplayName("Should return 404 when category does not exist during creation")
  void createAd_categoryNotFound_returns404() throws Exception {
    when(auth.getName()).thenReturn("test@test.com");

    AdCreateRequestDto request = new AdCreateRequestDto();
    request.setCategoryId(999L);
    request.setTitle("Title");
    request.setDescription("Description");
    request.setPrice(new BigDecimal("100"));

    when(adService.createAd(any(), any()))
        .thenThrow(new ResourceNotFoundException("Category not found"));

    mockMvc.perform(post("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isNotFound());
  }

  /**
   * Testing updateAd
   * */

  @Test
  @DisplayName("Should return 200 when ad is updated successfully")
  void updateAd_success() throws Exception {
    when(auth.getName()).thenReturn("test@test.com");

    AdUpdateRequestDto request = new AdUpdateRequestDto();
    request.setTitle("Updated title");
    request.setPrice(new BigDecimal("100.00"));
    request.setDescription("Updated description");

    AdResponseDto response = new AdResponseDto();
    response.setId(1L);
    response.setTitle("Updated title");

    when(adService.updateAd(eq(1L), any(), any())).thenReturn(response);

    mockMvc.perform(put("/api/ads/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated title"));
  }

  @Test
  @DisplayName("Should return 404 when ad not found during update")
  void updateAd_notFound() throws Exception {
    when(auth.getName()).thenReturn("test@test.com");

    AdUpdateRequestDto request = new AdUpdateRequestDto();
    request.setTitle("Updated title");
    request.setPrice(new BigDecimal("100.00"));
    request.setDescription("Updated description");

    when(adService.updateAd(eq(999L), any(), any()))
        .thenThrow(new ResourceNotFoundException("Ad not found"));

    mockMvc.perform(put("/api/ads/999")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Ad not found"));
  }

  @Test
  @DisplayName("Should return 403 when updating someone else's ad")
  void updateAd_notOwner_returns403() throws Exception {
    AdUpdateRequestDto request = new AdUpdateRequestDto();
    request.setTitle("Hacked title");
    request.setPrice(new BigDecimal("100.00"));
    request.setDescription("Hacked description");

    when(adService.updateAd(eq(1L), any(), any()))
        .thenThrow(new UnauthorizedActionException("Cannot edit ad: you are not the owner"));

    mockMvc.perform(put("/api/ads/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Cannot edit ad: you are not the owner"));
  }

  @Test
  @DisplayName("Should return 409 when updating ad with invalid status (SOLD/DELETED)")
  void updateAd_invalidStatus_returns409() throws Exception {
    AdUpdateRequestDto request = new AdUpdateRequestDto();
    request.setTitle("Updated title");
    request.setPrice(new BigDecimal("100.00"));
    request.setDescription("Updated description");

    when(adService.updateAd(eq(1L), any(), any()))
        .thenThrow(new InvalidAdStatusException("Cannot edit ad with status: SOLD"));

    mockMvc.perform(put("/api/ads/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isConflict());
  }

  /**
   * Testing deleteAd
   * */

  @Test
  @DisplayName("Should return 204 when ad is deleted")
  void deleteAd_success() throws Exception {
    when(auth.getName()).thenReturn("test@test.com");

    mockMvc.perform(delete("/api/ads/1")
            .principal(auth))
        .andExpect(status().isNoContent());
  }

  /**
   * Testing publishAd
   * */

  @Test
  @DisplayName("Should return 200 when ad is published by admin")
  void publishAd_success() throws Exception {
    when(auth.getName()).thenReturn("test@test.com");

    AdResponseDto response = new AdResponseDto();
    response.setId(1L);

    when(adService.publishAd(eq(1L), any())).thenReturn(response);

    mockMvc.perform(put("/api/ads/1/publish")
            .principal(auth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L));
  }
}
