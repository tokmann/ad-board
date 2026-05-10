package com.adboard.integration;

import com.adboard.dto.request.message.MessageRequestDto;
import com.adboard.entity.Ad;
import com.adboard.entity.Conversation;
import com.adboard.entity.User;
import com.adboard.entity.enums.AdStatus;
import com.adboard.entity.enums.ConversationStatus;
import com.adboard.entity.reference.Category;
import com.adboard.entity.reference.Role;
import com.adboard.repository.AdRepository;
import com.adboard.repository.CategoryRepository;
import com.adboard.repository.ConversationRepository;
import com.adboard.repository.MessageRepository;
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
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Personal messaging scenarios")
class MessageChatTest extends IntegrationTestBase {

  @Autowired
  private AdRepository adRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private ConversationRepository conversationRepository;

  @Autowired
  private MessageRepository messageRepository;

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
  private Ad testAd;
  private String buyerToken;

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

    buyer = new User();
    buyer.setUsername("Покупатель");
    buyer.setEmail("buyer@test.com");
    buyer.setPasswordHash(passwordEncoder.encode("password123"));
    buyer.setRoles(Set.of(roleUser));
    userRepository.save(buyer);

    buyerToken = loginAndGetToken("buyer@test.com", "password123");

    testAd = new Ad();
    testAd.setTitle("Товар для переписки");
    testAd.setDescription("Здесь будут тестировать сообщения");
    testAd.setCategory(electronics);
    testAd.setSeller(seller);
    testAd.setPrice(new BigDecimal("299.99"));
    adRepository.save(testAd);
    testAd.setStatus(AdStatus.ACTIVE);
    adRepository.save(testAd);
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
  @DisplayName("Should create new conversation and send first message when no chat exists")
  void sendMessage_firstMessage_createsConversationAndSavesMessage() throws Exception {
    MessageRequestDto request = new MessageRequestDto();
    request.setContent("Здравствуйте, товар еще в наличии?");

    String response = mockMvc.perform(post("/api/ads/{adId}/messages", testAd.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + buyerToken)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("Здравствуйте, товар еще в наличии?"))
        .andExpect(jsonPath("$.sender.username").value("Покупатель"))
        .andReturn().getResponse().getContentAsString();

    Long messageId = objectMapper.readTree(response).get("id").asLong();

    Conversation conv = conversationRepository.findByAdIdAndUserIds(testAd.getId(), buyer.getId(), seller.getId())
        .orElseThrow();

    assertThat(conv.getAd().getId()).isEqualTo(testAd.getId());
    assertThat(conv.getSeller().getId()).isEqualTo(seller.getId());
    assertThat(conv.getBuyer().getId()).isEqualTo(buyer.getId());
    assertThat(conv.getStatus()).isEqualTo(ConversationStatus.ACTIVE);

    var saved = messageRepository.findById(messageId).orElseThrow();
    assertThat(saved.getConversation().getId()).isEqualTo(conv.getId());
    assertThat(saved.getSender().getId()).isEqualTo(buyer.getId());
    assertThat(saved.getContent()).isEqualTo("Здравствуйте, товар еще в наличии?");
  }

  @Test
  @DisplayName("Should reuse existing conversation when sending follow-up message")
  void sendMessage_followUp_usesExistingConversation() throws Exception {
    MessageRequestDto first = new MessageRequestDto();
    first.setContent("Первое сообщение");

    mockMvc.perform(post("/api/ads/{adId}/messages", testAd.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + buyerToken)
            .content(objectMapper.writeValueAsString(first)))
        .andExpect(status().isCreated());

    Conversation existingConv = conversationRepository.findByAdIdAndUserIds(testAd.getId(), buyer.getId(), seller.getId())
        .orElseThrow();
    Long convId = existingConv.getId();

    MessageRequestDto second = new MessageRequestDto();
    second.setContent("Второе сообщение, продолжение");

    mockMvc.perform(post("/api/ads/{adId}/messages", testAd.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + buyerToken)
            .content(objectMapper.writeValueAsString(second)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("Второе сообщение, продолжение"));

    assertThat(conversationRepository.count()).isEqualTo(1);
    var messages = messageRepository.findByConversationId(convId, 0, 10);
    assertThat(messages).hasSize(2);
    assertThat(messages.get(1).getContent()).isEqualTo("Второе сообщение, продолжение");
  }

  @Test
  @DisplayName("Should return paginated chat history for existing conversation")
  void getMessages_withPagination_returnsConversationHistory() throws Exception {
    Conversation conv = conversationRepository.findByAdIdAndUserIds(testAd.getId(), buyer.getId(), seller.getId())
        .orElseGet(() -> {
          Conversation c = new Conversation();
          c.setAd(testAd);
          c.setSeller(seller);
          c.setBuyer(buyer);
          conversationRepository.save(c);
          return c;
        });

    for (int i = 1; i <= 5; i++) {
      var msg = new com.adboard.entity.Message();
      msg.setConversation(conv);
      msg.setSender(i % 2 == 0 ? seller : buyer);
      msg.setContent("Сообщение #" + i);
      messageRepository.save(msg);
    }

    mockMvc.perform(get("/api/ads/{adId}/messages", testAd.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + buyerToken)
            .param("withUserId", String.valueOf(seller.getId()))
            .param("page", "0")
            .param("size", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.totalElements").value(5))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.content[*].content").value(hasItems("Сообщение #1", "Сообщение #2", "Сообщение #3")));

    mockMvc.perform(get("/api/ads/{adId}/messages", testAd.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + buyerToken)
            .param("withUserId", String.valueOf(seller.getId()))
            .param("page", "1")
            .param("size", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[*].content").value(hasItems("Сообщение #4", "Сообщение #5")));
  }
}
