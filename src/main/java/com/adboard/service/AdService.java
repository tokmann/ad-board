package com.adboard.service;

import com.adboard.dto.request.ad.AdCreateRequestDto;
import com.adboard.dto.request.ad.AdUpdateRequestDto;
import com.adboard.dto.response.ad.AdResponseDto;
import com.adboard.dto.response.PageResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AdService {


  public PageResponse<AdResponseDto> searchAds(int page,
                                               int size,
                                               String keyword,
                                               Long categoryId,
                                               BigDecimal minPrice,
                                               BigDecimal maxPrice) {
    return null;
  }

  public AdResponseDto getAdById(Long id) {
    return null;
  }

  public AdResponseDto createAd(AdCreateRequestDto request, Authentication authentication) {
    return null;
  }

  public AdResponseDto updateAd(Long id, AdUpdateRequestDto request, Authentication authentication) {
    return null;
  }

  public void deleteAd(Long id, Authentication authentication) {
  }

  public AdResponseDto promoteAd(Long id, Authentication authentication) {
    return null;
  }
}
