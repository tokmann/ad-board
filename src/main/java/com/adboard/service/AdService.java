package com.adboard.service;

import com.adboard.dto.mapper.AdMapper;
import com.adboard.dto.request.ad.AdCreateRequestDto;
import com.adboard.dto.request.ad.AdUpdateRequestDto;
import com.adboard.dto.response.ad.AdResponseDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.entity.Ad;
import com.adboard.entity.User;
import com.adboard.entity.enums.AdStatus;
import com.adboard.entity.reference.Category;
import com.adboard.exception.AdAlreadyPromotedException;
import com.adboard.exception.InvalidAdStatusException;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.exception.UnauthorizedActionException;
import com.adboard.repository.AdRepository;
import com.adboard.repository.CategoryRepository;
import com.adboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdService {

  private final AdRepository adRepository;
  private final AdMapper adMapper;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;

  @Transactional(readOnly = true)
  public PageResponse<AdResponseDto> searchAds(int page,
                                               int size,
                                               String keyword,
                                               Long categoryId,
                                               BigDecimal minPrice,
                                               BigDecimal maxPrice) {
    if (page < 0) page = 0;
    if (size < 1 || size > 100) size = 10;

    long totalElements = adRepository.countSearch(keyword, categoryId, minPrice, maxPrice, AdStatus.ACTIVE);

    List<Ad> ads = adRepository.search(keyword, categoryId, minPrice, maxPrice, AdStatus.ACTIVE, page, size);

    List<AdResponseDto> content = ads.stream().map(adMapper::toResponseDto).toList();
    int totalPages = (int) Math.ceil((double) totalElements / size);

    log.info("Search completed: found {} ads (page {}/{}) for filters: keyword={}, cat={}",
        content.size(), page + 1, totalPages, keyword, categoryId);

    return new PageResponse<>(content, page, size, totalElements, totalPages);
  }

  @Transactional(readOnly = true)
  public AdResponseDto getAdById(Long id) {
    Ad ad = adRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Ad not found with id: " + id));

    return adMapper.toResponseDto(ad);
  }

  @Transactional
  public AdResponseDto createAd(AdCreateRequestDto request, Authentication authentication) {
    String sellerEmail = authentication.getName();

    User seller = userRepository.findByEmail(sellerEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerEmail));

    Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

    if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Price must be positive");
    }

    Ad ad = adMapper.toEntity(request);
    ad.setSeller(seller);
    ad.setCategory(category);

    adRepository.save(ad);

    log.info("Ad created successfully: id={}, seller={}", ad.getId(), sellerEmail);
    return adMapper.toResponseDto(ad);
  }

  @Transactional
  public AdResponseDto updateAd(Long id, AdUpdateRequestDto request, Authentication authentication) {
    String currentUserEmail = authentication.getName();

    Ad ad = adRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Ad not found with id: " + id));

    if (!ad.getSeller().getEmail().equals(currentUserEmail)) {
      throw new UnauthorizedActionException("Cannot edit ad: you are not the owner");
    }

    if (ad.getStatus() == AdStatus.SOLD || ad.getStatus() == AdStatus.DELETED) {
      throw new InvalidAdStatusException("Cannot edit ad with status: " + ad.getStatus());
    }

    adMapper.updateEntityFromDto(request, ad);

    ad.setStatus(AdStatus.DRAFT);
    ad.setUpdatedAt(LocalDateTime.now());
    adRepository.save(ad);

    log.info("Ad updated successfully: id={}, by={}", id, currentUserEmail);

    return adMapper.toResponseDto(ad);
  }

  @Transactional
  public void deleteAd(Long id, Authentication authentication) {
    String currentUserEmail = authentication.getName();

    Ad ad = adRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Ad not found with id: " + id));

    if (!ad.getSeller().getEmail().equals(currentUserEmail)) {
      throw new UnauthorizedActionException("Cannot delete ad: you are not the owner");
    }

    ad.setStatus(AdStatus.DELETED);
    ad.setUpdatedAt(LocalDateTime.now());

    adRepository.save(ad);

    log.info("Ad deleted successfully: id={}, by={}", id, currentUserEmail);
  }

  @Transactional
  public AdResponseDto promoteAd(Long adId, Authentication authentication) {
    String currentEmail = authentication.getName();

    Ad ad = adRepository.findById(adId)
        .orElseThrow(() -> new ResourceNotFoundException("Ad not found with id: " + adId));

    if (!ad.getSeller().getEmail().equals(currentEmail)) {
      throw new UnauthorizedActionException("You don't own this ad");
    }

    if (Boolean.TRUE.equals(ad.isPromoted())
        && ad.getPromoteExpiresAt() != null
        && ad.getPromoteExpiresAt().isAfter(LocalDateTime.now())) {
      throw new AdAlreadyPromotedException("Ad is already promoted");
    }

    ad.setPromoted(true);
    ad.setPromoteExpiresAt(LocalDateTime.now().plusDays(30));

    adRepository.save(ad);
    log.info("Ad {} successfully promoted for user {}. Expires at: {}",
        adId, currentEmail, ad.getPromoteExpiresAt());

    return adMapper.toResponseDto(ad);
  }

  @Transactional(readOnly = true)
  public PageResponse<AdResponseDto> getSalesHistory(int page, int size, Authentication authentication) {
    String sellerEmail = authentication.getName();

    long totalElements = adRepository.countSoldBySellerEmail(sellerEmail);

    List<Ad> ads = adRepository.findSoldBySellerEmail(sellerEmail, page, size);

    List<AdResponseDto> content = ads.stream()
        .map(adMapper::toResponseDto)
        .toList();

    int totalPages = (int) Math.ceil(totalElements / (double) size);

    log.info("Successfully retrieved sales history for user {}: {} ads (page {}/{})",
        sellerEmail, content.size(), page + 1, totalPages);

    return new PageResponse<>(content, page, size, totalElements, totalPages);
  }

  @Transactional
  public AdResponseDto publishAd(Long id, Authentication authentication) {
    Ad ad = adRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Ad not found with id: " + id));

    if (ad.getStatus() == AdStatus.ACTIVE) {
      throw new InvalidAdStatusException("Ad is already published and active");
    }
    if (ad.getStatus() == AdStatus.DELETED) {
      throw new InvalidAdStatusException("Cannot publish a deleted ad");
    }

    ad.setStatus(AdStatus.ACTIVE);
    ad.setUpdatedAt(LocalDateTime.now());
    adRepository.save(ad);

    log.info("Ad published by admin {}: id={}", authentication.getName(), id);
    return adMapper.toResponseDto(ad);
  }
}
