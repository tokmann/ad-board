package com.adboard.integration;

import com.adboard.dto.request.review.ReviewRequestDto;
import com.adboard.entity.Ad;
import com.adboard.entity.User;
import com.adboard.entity.enums.AdStatus;
import com.adboard.entity.reference.Category;
import com.adboard.entity.reference.Role;
import com.adboard.repository.AdRepository;
import com.adboard.repository.CategoryRepository;
import com.adboard.repository.ReviewRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Seller reviews and rating impact scenarios")
class ReviewAndRatingTest extends IntegrationTestBase {

  @Autowired
  private AdRepository adRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private Category electronics;
  private Role roleUser;

  @BeforeEach
  void setUp() {
    roleUser = roleRepository.findByName("ROLE_USER").orElseThrow();
    electronics = categoryRepository.findById(5L).orElseThrow();
  }

  private String loginAndGetToken(String email, String password) throws Exception {
    String loginRequest = """
            { "email": "%s", "password": "%s" }
            """.formatted(email, password);
    return mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequest))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString()
        .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
  }

  private Ad createAd(User seller, String title, BigDecimal price) {
    Ad ad = new Ad();
    ad.setTitle(title);
    ad.setDescription("Тестовое описание");
    ad.setCategory(electronics);
    ad.setSeller(seller);
    ad.setPrice(price);
    ad.setPromoted(false);
    ad.setCreatedAt(java.time.LocalDateTime.now());
    adRepository.save(ad);
    ad.setStatus(AdStatus.ACTIVE);
    adRepository.save(ad);
    return ad;
  }

  private void leaveReview(User seller, String reviewerEmail, String password, int rating, String comment) throws Exception {
    String token = loginAndGetToken(reviewerEmail, password);
    ReviewRequestDto request = new ReviewRequestDto();
    request.setRating(rating);
    request.setCommentText(comment);

    mockMvc.perform(post("/api/users/{sellerId}/reviews", seller.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("Should allow user to leave review for seller")
  void addReview_success_createsReview() throws Exception {
    User seller = new User();
    seller.setUsername("Тест продавец");
    seller.setEmail("seller-review@test.com");
    seller.setPasswordHash(passwordEncoder.encode("pass123"));
    seller.setRoles(Set.of(roleUser));
    userRepository.save(seller);

    User reviewer = new User();
    reviewer.setUsername("Тест рецензент");
    reviewer.setEmail("reviewer@test.com");
    reviewer.setPasswordHash(passwordEncoder.encode("pass123"));
    reviewer.setRoles(Set.of(roleUser));
    userRepository.save(reviewer);

    String reviewerToken = loginAndGetToken("reviewer@test.com", "pass123");

    ReviewRequestDto request = new ReviewRequestDto();
    request.setRating(4);
    request.setCommentText("Нормально");

    mockMvc.perform(post("/api/users/{sellerId}/reviews", seller.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + reviewerToken)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rating").value(4))
        .andExpect(jsonPath("$.commentText").value("Нормально"));

    var reviews = reviewRepository.findBySellerId(seller.getId(), 0, 10);
    assertThat(reviews).extracting("rating").contains(4);
    assertThat(reviews.get(0).getReviewer().getEmail()).isEqualTo("reviewer@test.com");
  }

  @Test
  @DisplayName("Should reject review when user tries to review themselves")
  void addReview_selfReview_returnsBadRequest() throws Exception {
    User selfUser = new User();
    selfUser.setUsername("Сам себе");
    selfUser.setEmail("self@test.com");
    selfUser.setPasswordHash(passwordEncoder.encode("pass123"));
    selfUser.setRoles(Set.of(roleUser));
    userRepository.save(selfUser);

    String token = loginAndGetToken("self@test.com", "pass123");

    ReviewRequestDto request = new ReviewRequestDto();
    request.setRating(5);
    request.setCommentText("Я молодец");

    mockMvc.perform(post("/api/users/{sellerId}/reviews", selfUser.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    assertThat(reviewRepository.findBySellerId(selfUser.getId(), 0, 10)).isEmpty();
  }

  @Test
  @DisplayName("Should reject duplicate review from same user")
  void addReview_duplicate_returnsConflict() throws Exception {
    User seller = new User();
    seller.setUsername("Дубликат продавец");
    seller.setEmail("dup-seller@test.com");
    seller.setPasswordHash(passwordEncoder.encode("pass123"));
    seller.setRoles(Set.of(roleUser));
    userRepository.save(seller);

    User reviewer = new User();
    reviewer.setUsername("Дубликат рецензент");
    reviewer.setEmail("dup-rev@test.com");
    reviewer.setPasswordHash(passwordEncoder.encode("pass123"));
    reviewer.setRoles(Set.of(roleUser));
    userRepository.save(reviewer);

    String reviewerToken = loginAndGetToken("dup-rev@test.com", "pass123");

    ReviewRequestDto request = new ReviewRequestDto();
    request.setRating(5);
    request.setCommentText("Первый");

    mockMvc.perform(post("/api/users/{sellerId}/reviews", seller.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + reviewerToken)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    request.setCommentText("Второй");
    mockMvc.perform(post("/api/users/{sellerId}/reviews", seller.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + reviewerToken)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());

    var reviews = reviewRepository.findBySellerId(seller.getId(), 0, 10);
    assertThat(reviews).hasSize(1);
    assertThat(reviews.get(0).getCommentText()).isEqualTo("Первый");
  }

  @Test
  @DisplayName("Should show ads from higher-rated sellers first in search")
  void searchAds_higherRatedSellersAppearFirst() throws Exception {
    User sellerHigh = new User();
    sellerHigh.setUsername("Хороший продавец");
    sellerHigh.setEmail("good-rating@test.com");
    sellerHigh.setPasswordHash(passwordEncoder.encode("pass123"));
    sellerHigh.setRoles(Set.of(roleUser));
    userRepository.save(sellerHigh);

    User sellerLow = new User();
    sellerLow.setUsername("Плохой продавец");
    sellerLow.setEmail("bad-rating@test.com");
    sellerLow.setPasswordHash(passwordEncoder.encode("pass123"));
    sellerLow.setRoles(Set.of(roleUser));
    userRepository.save(sellerLow);

    Ad adHigh = createAd(sellerHigh, "Товар от хорошего", new BigDecimal("100.00"));
    Ad adLow = createAd(sellerLow, "Товар от плохого", new BigDecimal("100.00"));

    User rev1 = new User();
    rev1.setUsername("Рев1");
    rev1.setEmail("rev1@test.com");
    rev1.setPasswordHash(passwordEncoder.encode("pass123"));
    rev1.setRoles(Set.of(roleUser));
    userRepository.save(rev1);

    User rev2 = new User();
    rev2.setUsername("Рев2");
    rev2.setEmail("rev2@test.com");
    rev2.setPasswordHash(passwordEncoder.encode("pass123"));
    rev2.setRoles(Set.of(roleUser));
    userRepository.save(rev2);

    leaveReview(sellerHigh, "rev1@test.com", "pass123", 5, "Отлично!");
    leaveReview(sellerHigh, "rev2@test.com", "pass123", 5, "Супер!");
    leaveReview(sellerLow, "rev1@test.com", "pass123", 1, "Ужасно");
    leaveReview(sellerLow, "rev2@test.com", "pass123", 1, "Не рекомендую");

    String searcherToken = loginAndGetToken("rev1@test.com", "pass123");

    mockMvc.perform(get("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + searcherToken)
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].seller.username").value("Хороший продавец"))
        .andExpect(jsonPath("$.content[0].title").value("Товар от хорошего"))
        .andExpect(jsonPath("$.content[1].seller.username").value("Плохой продавец"))
        .andExpect(jsonPath("$.content[1].title").value("Товар от плохого"));
  }
}