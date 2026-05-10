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
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Ad search and filtering scenarios")
class AdSearchAndFilterTest extends IntegrationTestBase {

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
  private Category electronics;
  private Category furniture;
  private String validToken;

  @BeforeEach
  void setUp() throws Exception {
    Role roleUser = roleRepository.findByName("ROLE_USER").orElseThrow();

    seller = new User();
    seller.setUsername("Тестовый продавец");
    seller.setEmail("seller@test.com");
    seller.setPasswordHash(passwordEncoder.encode("password123"));
    seller.setPhone("+79990001111");
    seller.setCity("Москва");
    Set<Role> roles = new HashSet<>();
    roles.add(roleUser);
    seller.setRoles(roles);
    userRepository.save(seller);

    String loginRequest = """
            {
              "email": "seller@test.com",
              "password": "password123"
            }
            """;
    validToken = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequest))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
        .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

    electronics = categoryRepository.findById(1L).orElseThrow();
    furniture = categoryRepository.findById(5L).orElseThrow();

    createAd("iPhone 15 Pro", "Новый телефон от Apple, 256 ГБ", electronics, new BigDecimal("999.99"));
    createAd("iPhone 14", "Прошлое поколение, отличное состояние", electronics, new BigDecimal("799.99"));
    createAd("MacBook Pro M3", "Мощный ноутбук для работы и игр", electronics, new BigDecimal("2499.00"));
    createAd("Samsung Galaxy S24", "Флагман на Android", electronics, new BigDecimal("899.00"));
    createAd("iPad Air", "Планшет для учебы и развлечений", electronics, new BigDecimal("599.00"));
    createAd("Наушники Sony WH-1000XM5", "Шумоподавление, беспроводные", electronics, new BigDecimal("349.99"));
    createAd("Монитор Dell UltraSharp", "4K дисплей для профессионалов", electronics, new BigDecimal("449.00"));
    createAd("Письменный стол", "Деревянный стол для офиса", furniture, new BigDecimal("299.00"));
    createAd("Офисное кресло", "Эргономичное, с поддержкой поясницы", furniture, new BigDecimal("199.99"));
    createAd("Книжная полка", "5 ярусов, сосна", furniture, new BigDecimal("89.99"));
    createAd("Журнальный столик", "Стеклянная столешница", furniture, new BigDecimal("149.00"));
    createAd("Напольная лампа", "Современная светодиодная", furniture, new BigDecimal("79.99"));
    createAd("Беспроводная мышь", "Bluetooth, эргономичная", electronics, new BigDecimal("29.99"));
    createAd("Механическая клавиатура", "RGB подсветка, геймерская", electronics, new BigDecimal("129.00"));
    createAd("USB-C хаб", "7-в-1 адаптер для ноутбука", electronics, new BigDecimal("49.99"));
  }

  private void createAd(String title, String description, Category category, BigDecimal price) {
    Ad ad = new Ad();
    ad.setTitle(title);
    ad.setDescription(description);
    ad.setCategory(category);
    ad.setSeller(seller);
    ad.setPrice(price);
    ad.setImageUrl("https://example.com/image.jpg");
    adRepository.save(ad);
    ad.setStatus(AdStatus.ACTIVE);
    adRepository.save(ad);
  }

  @Test
  @DisplayName("Should return paginated list of active ads with correct metadata")
  void searchAds_withNoFilters_returnsPaginatedActiveAds() throws Exception {
    mockMvc.perform(get("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + validToken)
            .param("page", "0")
            .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(5))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(15))
        .andExpect(jsonPath("$.totalPages").value(3))
        .andExpect(jsonPath("$.content[*].title").exists())
        .andExpect(jsonPath("$.content[*].price").exists())
        .andExpect(jsonPath("$.content[*].category.name").exists());
  }

  @Test
  @DisplayName("Should filter ads by keyword in title or description")
  void searchAds_withKeywordFilter_returnsMatchingAds() throws Exception {
    mockMvc.perform(get("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + validToken)
            .param("keyword", "iPhone"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(content().json("""
                {
                  "content": [{
                    "title": "iPhone 15 Pro",
                    "description": "Новый телефон от Apple, 256 ГБ"
                  }, {
                    "title": "iPhone 14",
                    "description": "Прошлое поколение, отличное состояние"
                  }]
                }
                """, false));
  }

  @Test
  @DisplayName("Should filter ads by category and price range")
  void searchAds_withCategoryAndPriceFilters_returnsFilteredAds() throws Exception {
    mockMvc.perform(get("/api/ads")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + validToken)
            .param("categoryId", String.valueOf(electronics.getId()))
            .param("minPrice", "100")
            .param("maxPrice", "500"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.content[?(@.price == 349.99)].category.name").value("Электроника"))
        .andExpect(jsonPath("$.content[?(@.price == 449.00)].category.name").value("Электроника"))
        .andExpect(jsonPath("$.content[?(@.price == 129.00)].category.name").value("Электроника"));

    var ads = adRepository.search("null", electronics.getId(), new BigDecimal("100"), new BigDecimal("500"), AdStatus.ACTIVE, 0, 10);
    ads.forEach(ad -> {
      assertThat(ad.getCategory().getId()).isEqualTo(electronics.getId());
      assertThat(ad.getPrice()).isBetween(new BigDecimal("100"), new BigDecimal("500"));
      assertThat(ad.getStatus()).isEqualTo(AdStatus.ACTIVE);
    });
  }
}