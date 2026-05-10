package com.adboard.integration;

import com.adboard.dto.request.ad.AdCreateRequestDto;
import com.adboard.dto.request.ad.AdUpdateRequestDto;
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
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Ad management lifecycle scenarios")
class AdManagementTest extends IntegrationTestBase {

  @Autowired
  private AdRepository adRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User seller;
  private User admin;
  private Category electronics;
  private String sellerToken;
  private String adminToken;
  private Long createdAdId;

  @BeforeEach
  void setUp() throws Exception {
    Role roleUser = roleRepository.findByName("ROLE_USER").orElseThrow();
    Role roleAdmin = roleRepository.findByName("ROLE_ADMIN").orElseThrow();

    seller = new User();
    seller.setUsername("Продавец");
    seller.setEmail("seller@test.com");
    seller.setPasswordHash(passwordEncoder.encode("password123"));
    seller.setPhone("+79990001111");
    seller.setCity("Москва");
    Set<Role> sellerRoles = new HashSet<>();
    sellerRoles.add(roleUser);
    seller.setRoles(sellerRoles);
    userRepository.save(seller);

    admin = new User();
    admin.setUsername("Админ");
    admin.setEmail("admin@test.com");
    admin.setPasswordHash(passwordEncoder.encode("admin123"));
    admin.setPhone("+79990002222");
    admin.setCity("Санкт-Петербург");
    Set<Role> adminRoles = new HashSet<>();
    adminRoles.add(roleUser);
    adminRoles.add(roleAdmin);
    admin.setRoles(adminRoles);
    userRepository.save(admin);

    sellerToken = loginAndGetToken("seller@test.com", "password123");
    adminToken = loginAndGetToken("admin@test.com", "admin123");

    electronics = categoryRepository.findById(5L).orElseThrow();
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
        .andReturn()
        .getResponse()
        .getContentAsString()
        .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
  }

  @Test
  @DisplayName("Should create ad in DRAFT status when owner submits")
  void createAd_owner_success_returnsDraftAd() throws Exception {
    AdCreateRequestDto request = new AdCreateRequestDto();
    request.setTitle("iPhone 15 Pro");
    request.setDescription("Новый телефон, 256 ГБ, гарантия");
    request.setCategoryId(electronics.getId());
    request.setPrice(new BigDecimal("999.99"));
    request.setImageUrl("https://example.com/iphone.jpg");

    String response = mockMvc.perform(post("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + sellerToken)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.title").value("iPhone 15 Pro"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andReturn().getResponse().getContentAsString();

    createdAdId = objectMapper.readTree(response).get("id").asLong();

    Ad saved = adRepository.findById(createdAdId).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(AdStatus.DRAFT);
    assertThat(saved.getSeller().getId()).isEqualTo(seller.getId());
    assertThat(saved.getCategory().getId()).isEqualTo(electronics.getId());
    assertThat(saved.getPrice()).isEqualTo(new BigDecimal("999.99"));
  }

  @Test
  @DisplayName("Should update ad fields and keep DRAFT status when owner edits")
  void updateAd_owner_success_updatesFieldsAndReturnsToDraft() throws Exception {
    AdCreateRequestDto createRequest = new AdCreateRequestDto();
    createRequest.setTitle("Старое название");
    createRequest.setDescription("Старое описание");
    createRequest.setCategoryId(electronics.getId());
    createRequest.setPrice(new BigDecimal("500.00"));
    createRequest.setImageUrl("https://example.com/old.jpg");

    String createResponse = mockMvc.perform(post("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + sellerToken)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long adId = objectMapper.readTree(createResponse).get("id").asLong();

    AdUpdateRequestDto updateRequest = new AdUpdateRequestDto();
    updateRequest.setTitle("iPhone 15 Pro — Обновленное");
    updateRequest.setDescription("Новое описание с деталями");
    updateRequest.setPrice(new BigDecimal("899.99"));

    mockMvc.perform(put("/api/ads/{id}", adId)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + sellerToken)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(content().json("""
                {
                  "title": "iPhone 15 Pro — Обновленное",
                  "description": "Новое описание с деталями",
                  "price": 899.99,
                  "status": "DRAFT"
                }
                """, false));

    Ad updated = adRepository.findById(adId).orElseThrow();
    assertThat(updated.getTitle()).isEqualTo("iPhone 15 Pro — Обновленное");
    assertThat(updated.getDescription()).isEqualTo("Новое описание с деталями");
    assertThat(updated.getPrice()).isEqualTo(new BigDecimal("899.99"));
    assertThat(updated.getStatus()).isEqualTo(AdStatus.DRAFT);
  }

  @Test
  @DisplayName("Should soft-delete ad when owner requests deletion")
  void deleteAd_owner_success_softDeletes() throws Exception {
    AdCreateRequestDto request = new AdCreateRequestDto();
    request.setTitle("Удаляемый товар");
    request.setDescription("Будет удален");
    request.setCategoryId(electronics.getId());
    request.setPrice(new BigDecimal("100.00"));

    String createResponse = mockMvc.perform(post("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + sellerToken)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long adId = objectMapper.readTree(createResponse).get("id").asLong();

    mockMvc.perform(delete("/api/ads/{id}", adId)
            .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isNoContent());

    Ad deleted = adRepository.findById(adId).orElseThrow();
    assertThat(deleted.getStatus()).isEqualTo(AdStatus.DELETED);

    mockMvc.perform(get("/api/ads")
            .header("Authorization", "Bearer " + sellerToken)
            .param("keyword", "Удаляемый"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  @DisplayName("Should publish ad to ACTIVE status when admin approves")
  void publishAd_admin_success_activatesAd() throws Exception {
    AdCreateRequestDto createRequest = new AdCreateRequestDto();
    createRequest.setTitle("Товар на модерацию");
    createRequest.setDescription("Ждет одобрения админа");
    createRequest.setCategoryId(electronics.getId());
    createRequest.setPrice(new BigDecimal("299.99"));

    String createResponse = mockMvc.perform(post("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + sellerToken)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long adId = objectMapper.readTree(createResponse).get("id").asLong();

    mockMvc.perform(put("/api/ads/{id}/publish", adId)
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(content().json("""
                {
                  "title": "Товар на модерацию",
                  "status": "ACTIVE"
                }
                """, false));

    Ad published = adRepository.findById(adId).orElseThrow();
    assertThat(published.getStatus()).isEqualTo(AdStatus.ACTIVE);

    mockMvc.perform(get("/api/ads")
            .header("Authorization", "Bearer " + sellerToken)
            .param("keyword", "модерацию"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }
}