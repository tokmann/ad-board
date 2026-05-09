package com.adboard.unit.service;

import com.adboard.dto.mapper.AdMapper;
import com.adboard.dto.request.ad.AdCreateRequestDto;
import com.adboard.dto.request.ad.AdUpdateRequestDto;
import com.adboard.dto.response.ad.AdResponseDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.entity.Ad;
import com.adboard.entity.User;
import com.adboard.entity.enums.AdStatus;
import com.adboard.entity.reference.Category;
import com.adboard.exception.*;
import com.adboard.repository.AdRepository;
import com.adboard.repository.CategoryRepository;
import com.adboard.repository.UserRepository;
import com.adboard.service.AdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdServiceTest {

  @Mock
  private AdRepository adRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private AdMapper adMapper;

  @InjectMocks
  private AdService adService;

  private User seller;
  private Category category;
  private Ad ad;
  private Authentication auth;

  @BeforeEach
  void setUp() {
    seller = new User();
    seller.setEmail("seller@test.com");
    seller.setId(1L);

    category = new Category();
    category.setId(1L);
    category.setName("Electronics");

    ad = new Ad();
    ad.setId(10L);
    ad.setSeller(seller);
    ad.setCategory(category);
    ad.setStatus(AdStatus.DRAFT);
    ad.setPrice(BigDecimal.valueOf(100));
    ad.setPromoted(false);
    ad.setTitle("Original Title");
    ad.setDescription("Original description");
    ad.setUpdatedAt(LocalDateTime.now());

    auth = mock(Authentication.class);
  }

  /**
   * Testing searchAds
   * */

  @Test
  @DisplayName("Should return paginated active ads with correct total count")
  void searchAds_returnsPaginatedActiveAds() {
    when(adRepository.countSearch(any(), any(), any(), any(), eq(AdStatus.ACTIVE))).thenReturn(25L);
    when(adRepository.search(any(), any(), any(), any(), eq(AdStatus.ACTIVE), eq(0), eq(10)))
        .thenReturn(List.of(ad));
    when(adMapper.toResponseDto(any())).thenReturn(new AdResponseDto());

    PageResponse<AdResponseDto> result = adService.searchAds(0, 10, null, null, null, null);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalPages()).isEqualTo(3);
    assertThat(result.getTotalElements()).isEqualTo(25);
  }

  @Test
  @DisplayName("Should filter ads by keyword and call repository")
  void searchAds_filtersByKeyword() {
    when(adRepository.countSearch(eq("iphone"), any(), any(), any(), eq(AdStatus.ACTIVE))).thenReturn(1L);
    when(adRepository.search(eq("iphone"), any(), any(), any(), eq(AdStatus.ACTIVE), eq(0), eq(10)))
        .thenReturn(List.of(ad));
    when(adMapper.toResponseDto(any())).thenReturn(new AdResponseDto());

    PageResponse<AdResponseDto> result = adService.searchAds(0, 10, "iphone", null, null, null);

    assertThat(result.getContent()).hasSize(1);
    verify(adRepository).countSearch(eq("iphone"), any(), any(), any(), eq(AdStatus.ACTIVE));
  }

  @Test
  @DisplayName("Should correct negative page and invalid size to default values")
  void searchAds_handlesInvalidPagination() {
    when(adRepository.countSearch(any(), any(), any(), any(), eq(AdStatus.ACTIVE))).thenReturn(5L);
    when(adRepository.search(any(), any(), any(), any(), eq(AdStatus.ACTIVE), eq(0), eq(10)))
        .thenReturn(List.of(ad));
    when(adMapper.toResponseDto(any())).thenReturn(new AdResponseDto());

    PageResponse<AdResponseDto> result = adService.searchAds(-5, 0, null, null, null, null);

    assertThat(result.getPage()).isEqualTo(0);
    assertThat(result.getSize()).isEqualTo(10);
  }

  /**
   * Testing getAdById
   * */

  @Test
  @DisplayName("Should return ad response when active ad is found")
  void getAdById_returnsActiveAd() {
    when(adRepository.findActiveById(1L)).thenReturn(Optional.of(ad));
    when(adMapper.toResponseDto(ad)).thenReturn(new AdResponseDto());

    AdResponseDto result = adService.getAdById(1L);

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Should throw exception when ad is not found or not active")
  void getAdById_throws_whenNotFound() {
    when(adRepository.findActiveById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adService.getAdById(999L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  /**
   * Testing createAd
   * */

  @Test
  @DisplayName("Should throw exception when price is zero or negative and not save ad")
  void createAd_throws_whenPriceInvalid() {
    when(auth.getName()).thenReturn("seller@test.com");

    AdCreateRequestDto request = new AdCreateRequestDto();
    request.setCategoryId(1L);
    request.setPrice(BigDecimal.ZERO);

    when(userRepository.findByEmail("seller@test.com")).thenReturn(Optional.of(seller));
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

    assertThatThrownBy(() -> adService.createAd(request, auth))
        .isInstanceOf(IllegalArgumentException.class);

    verify(adRepository, never()).save(any());
  }

  /**
   * Testing updateAd
   * */

  @Test
  @DisplayName("Should throw exception when non-owner tries to edit")
  void updateAd_throws_whenNotOwner() {
    when(auth.getName()).thenReturn("stranger@test.com");
    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));

    assertThatThrownBy(() -> adService.updateAd(10L, new AdUpdateRequestDto(), auth))
        .isInstanceOf(UnauthorizedActionException.class);
  }

  /**
   * Testing deleteAd
   * */

  @Test
  @DisplayName("Should perform soft delete by setting status to DELETED when owner deletes")
  void deleteAd_owner_softDeletes() {
    when(auth.getName()).thenReturn("seller@test.com");

    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));

    adService.deleteAd(10L, auth);

    ArgumentCaptor<Ad> adCaptor = ArgumentCaptor.forClass(Ad.class);
    verify(adRepository).save(adCaptor.capture());
    assertThat(adCaptor.getValue().getStatus()).isEqualTo(AdStatus.DELETED);
  }

  @Test
  @DisplayName("Should throw exception when non-owner tries to delete")
  void deleteAd_throws_whenNotOwner() {
    when(auth.getName()).thenReturn("stranger@test.com");
    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));

    assertThatThrownBy(() -> adService.deleteAd(10L, auth))
        .isInstanceOf(UnauthorizedActionException.class);

    verify(adRepository, never()).save(any());
  }

  /**
   * Testing promoteAd
   * */

  @Test
  @DisplayName("Should activate promotion and set expiration date for valid ad")
  void promoteAd_valid_activatesPromotion() {
    when(auth.getName()).thenReturn("seller@test.com");

    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
    when(adMapper.toResponseDto(ad)).thenReturn(new AdResponseDto());

    adService.promoteAd(10L, auth);

    ArgumentCaptor<Ad> adCaptor = ArgumentCaptor.forClass(Ad.class);
    verify(adRepository).save(adCaptor.capture());
    Ad saved = adCaptor.getValue();

    assertThat(saved.isPromoted()).isTrue();
    assertThat(saved.getPromoteExpiresAt()).isAfter(LocalDateTime.now());
  }

  @Test
  @DisplayName("Should throw conflict exception when ad is already promoted and active")
  void promoteAd_throws_whenAlreadyPromoted() {
    when(auth.getName()).thenReturn("seller@test.com");

    ad.setPromoted(true);
    ad.setPromoteExpiresAt(LocalDateTime.now().plusDays(10));

    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));

    assertThatThrownBy(() -> adService.promoteAd(10L, auth))
        .isInstanceOf(AdAlreadyPromotedException.class);
  }

  /**
   * Testing getSalesHistory
   * */

  @Test
  @DisplayName("Should return paginated history containing only sold ads")
  void getSalesHistory_returnsOnlySoldAds() {
    when(auth.getName()).thenReturn("seller@test.com");

    when(adRepository.countSoldBySellerEmail("seller@test.com")).thenReturn(3L);
    when(adRepository.findSoldBySellerEmail("seller@test.com", 0, 10)).thenReturn(List.of(ad));
    when(adMapper.toResponseDto(any())).thenReturn(new AdResponseDto());

    PageResponse<AdResponseDto> result = adService.getSalesHistory(0, 10, auth);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(3);
  }

  /**
   * Testing publishAd
   * */

  @Test
  @DisplayName("Should change status from DRAFT to ACTIVE when admin publishes")
  void publishAd_draft_activates() {
    ad.setStatus(AdStatus.DRAFT);
    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
    when(adMapper.toResponseDto(ad)).thenReturn(new AdResponseDto());

    adService.publishAd(10L, auth);

    ArgumentCaptor<Ad> adCaptor = ArgumentCaptor.forClass(Ad.class);
    verify(adRepository).save(adCaptor.capture());
    assertThat(adCaptor.getValue().getStatus()).isEqualTo(AdStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should throw exception when trying to publish already active ad")
  void publishAd_throws_whenAlreadyActive() {
    ad.setStatus(AdStatus.ACTIVE);

    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));

    assertThatThrownBy(() -> adService.publishAd(10L, auth))
        .isInstanceOf(InvalidAdStatusException.class);
  }

  @Test
  @DisplayName("Should throw exception when trying to publish DELETED ad")
  void publishAd_throws_whenAdIsDeleted() {
    ad.setStatus(AdStatus.DELETED);
    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));

    assertThatThrownBy(() -> adService.publishAd(10L, auth))
        .isInstanceOf(InvalidAdStatusException.class);
  }
}
