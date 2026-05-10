package com.adboard.integration;

import com.adboard.entity.Ad;
import com.adboard.entity.User;
import com.adboard.entity.enums.AdStatus;
import com.adboard.entity.reference.Category;
import com.adboard.entity.reference.Role;
import com.adboard.repository.AdRepository;
import com.adboard.repository.CategoryRepository;
import com.adboard.repository.RoleRepository;
import com.adboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Ad promotion scenarios")
class AdPromotionTest extends IntegrationTestBase {

  @Autowired
  private AdRepository adRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User seller;
  private User otherUser;
  private Category electronics;
  private Role roleUser;
  private String sellerToken;
  private String otherToken;

  @BeforeEach
  void setUp() throws Exception {
    roleUser = roleRepository.findByName("ROLE_USER").orElseThrow();
    electronics = categoryRepository.findById(5L).orElseThrow();

    seller = new User();
    seller.setUsername("Продавец");
    seller.setEmail("seller@test.com");
    seller.setPasswordHash(passwordEncoder.encode("password123"));
    seller.setRoles(Set.of(roleUser));
    userRepository.save(seller);

    otherUser = new User();
    otherUser.setUsername("Другой");
    otherUser.setEmail("other@test.com");
    otherUser.setPasswordHash(passwordEncoder.encode("password123"));
    otherUser.setRoles(Set.of(roleUser));
    userRepository.save(otherUser);

    sellerToken = loginAndGetToken("seller@test.com", "password123");
    otherToken = loginAndGetToken("other@test.com", "password123");
  }

  private String loginAndGetToken(String email, String password) throws Exception {
    String loginRequest = """
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);

    return mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequest))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString()
        .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
  }

  @Test
  @DisplayName("Should promote ad when owner requests and ad is not already promoted")
  void promoteAd_owner_success_activatesPromotion() throws Exception {
    Ad ad = createAd(seller, "Обычное объявление", new BigDecimal("199.99"), false, null);

    mockMvc.perform(post("/api/ads/{adId}/promote", ad.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ad.getId()))
        .andExpect(jsonPath("$.promoted").value(true))
        .andExpect(jsonPath("$.promoteExpiresAt").exists());

    Ad promoted = adRepository.findById(ad.getId()).orElseThrow();
    assertThat(promoted.isPromoted()).isTrue();
    assertThat(promoted.getPromoteExpiresAt()).isAfter(LocalDateTime.now());
    assertThat(promoted.getPromoteExpiresAt()).isBefore(LocalDateTime.now().plusDays(31));
  }

  @Test
  @DisplayName("Should reject promotion when non-owner tries to promote")
  void promoteAd_nonOwner_returnsForbidden() throws Exception {
    Ad ad = createAd(seller, "Чужое объявление", new BigDecimal("299.99"), false, null);

    mockMvc.perform(post("/api/ads/{adId}/promote", ad.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isForbidden());

    Ad unchanged = adRepository.findById(ad.getId()).orElseThrow();
    assertThat(unchanged.isPromoted()).isFalse();
    assertThat(unchanged.getPromoteExpiresAt()).isNull();
  }

  @Test
  @DisplayName("Should reject promotion when ad is already actively promoted")
  void promoteAd_alreadyPromoted_returnsConflict() throws Exception {
    Ad ad = createAd(seller, "Уже промотировано", new BigDecimal("399.99"), true, LocalDateTime.now().plusDays(15));

    mockMvc.perform(post("/api/ads/{adId}/promote", ad.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isConflict());

    Ad unchanged = adRepository.findById(ad.getId()).orElseThrow();
    assertThat(unchanged.getPromoteExpiresAt()).isBefore(LocalDateTime.now().plusDays(16));
  }

  @Test
  @DisplayName("Should show promoted ads first in search results regardless of createdAt")
  void searchAds_promotedAppearFirst_inSearchResults() throws Exception {

    Ad oldPromoted = createAd(seller, "Старый промотированный", new BigDecimal("100.00"), true, LocalDateTime.now().plusDays(10));
    oldPromoted.setCreatedAt(LocalDateTime.now().minusDays(10));
    adRepository.save(oldPromoted);

    Ad newNormal = createAd(seller, "Новый обычный", new BigDecimal("200.00"), false, null);
    newNormal.setCreatedAt(LocalDateTime.now());
    adRepository.save(newNormal);

    Ad midNormal = createAd(seller, "Средний обычный", new BigDecimal("150.00"), false, null);
    midNormal.setCreatedAt(LocalDateTime.now().minusDays(5));
    adRepository.save(midNormal);

    mockMvc.perform(get("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + sellerToken)
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Старый промотированный"))
        .andExpect(jsonPath("$.content[0].promoted").value(true));
  }

  private Ad createAd(User owner, String title, BigDecimal price, boolean promoted, LocalDateTime expiresAt) {
    Ad ad = new Ad();
    ad.setTitle(title);
    ad.setDescription("Тестовое описание");
    ad.setCategory(electronics);
    ad.setSeller(owner);
    ad.setPrice(price);
    ad.setPromoted(promoted);
    ad.setPromoteExpiresAt(expiresAt);
    adRepository.save(ad);
    ad.setStatus(AdStatus.ACTIVE);
    adRepository.save(ad);
    return ad;
  }
}