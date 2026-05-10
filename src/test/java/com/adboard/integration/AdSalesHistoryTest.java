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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("User sales history scenarios")
class AdSalesHistoryTest extends IntegrationTestBase {

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
  private User buyer;
  private Category electronics;
  private Role roleUser;
  private String sellerToken;
  private String buyerToken;

  @BeforeEach
  void setUp() throws Exception {
    roleUser = roleRepository.findByName("ROLE_USER").orElseThrow();
    electronics = categoryRepository.findById(5L).orElseThrow();

    seller = new User();
    seller.setUsername("Продавец");
    seller.setEmail("seller-sales@test.com");
    seller.setPasswordHash(passwordEncoder.encode("pass123"));
    seller.setRoles(Set.of(roleUser));
    userRepository.save(seller);

    buyer = new User();
    buyer.setUsername("Покупатель");
    buyer.setEmail("buyer-sales@test.com");
    buyer.setPasswordHash(passwordEncoder.encode("pass123"));
    buyer.setRoles(Set.of(roleUser));
    userRepository.save(buyer);

    sellerToken = loginAndGetToken("seller-sales@test.com", "pass123");
    buyerToken = loginAndGetToken("buyer-sales@test.com", "pass123");
  }

  private String loginAndGetToken(String email, String password) throws Exception {
    String loginRequest = """
            { 
            "email": "%s", 
            "password": "%s" 
            }
            """.formatted(email, password);
    return mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON).content(loginRequest))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString()
        .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
  }

  private Ad createActiveAd(User owner, String title, BigDecimal price) {
    Ad ad = new Ad();
    ad.setTitle(title);
    ad.setDescription("Тест");
    ad.setCategory(electronics);
    ad.setSeller(owner);
    ad.setPrice(price);
    adRepository.save(ad);
    ad.setStatus(AdStatus.ACTIVE);
    adRepository.save(ad);
    return ad;
  }

  @Test
  @DisplayName("Should allow owner to mark their ad as sold")
  void markAdAsSold_owner_success_changesStatus() throws Exception {
    Ad ad = createActiveAd(seller, "Товар для продажи", new BigDecimal("500.00"));

    mockMvc.perform(put("/api/ads/{id}/sold", ad.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SOLD"))
        .andExpect(jsonPath("$.soldAt").exists());

    Ad sold = adRepository.findById(ad.getId()).orElseThrow();
    assertThat(sold.getStatus()).isEqualTo(AdStatus.SOLD);
    assertThat(sold.getSoldAt()).isNotNull();
  }

  @Test
  @DisplayName("Should reject marking ad as sold when user is not owner")
  void markAdAsSold_nonOwner_returnsForbidden() throws Exception {
    Ad ad = createActiveAd(seller, "Чужой товар", new BigDecimal("600.00"));

    mockMvc.perform(put("/api/ads/{id}/sold", ad.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + buyerToken))
        .andExpect(status().isForbidden());

    Ad unchanged = adRepository.findById(ad.getId()).orElseThrow();
    assertThat(unchanged.getStatus()).isEqualTo(AdStatus.ACTIVE);
    assertThat(unchanged.getSoldAt()).isNull();
  }

  @Test
  @DisplayName("Should exclude sold ads from regular search results")
  void searchAds_excludesSoldAds_byDefault() throws Exception {
    Ad active = createActiveAd(seller, "Активный товар", new BigDecimal("100.00"));
    Ad sold = createActiveAd(seller, "Проданный товар", new BigDecimal("200.00"));

    mockMvc.perform(put("/api/ads/{id}/sold", sold.getId())
            .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + buyerToken)
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.title == 'Активный товар')]").exists())
        .andExpect(jsonPath("$.content[?(@.title == 'Проданный товар')]").doesNotExist())
        .andExpect(jsonPath("$.totalElements").value(1));
  }
}
