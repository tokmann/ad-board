package com.adboard.integration;

import com.adboard.dto.request.comment.CommentRequestDto;
import com.adboard.entity.Ad;
import com.adboard.entity.Comment;
import com.adboard.entity.User;
import com.adboard.entity.enums.AdStatus;
import com.adboard.entity.reference.Category;
import com.adboard.entity.reference.Role;
import com.adboard.repository.AdRepository;
import com.adboard.repository.CategoryRepository;
import com.adboard.repository.CommentRepository;
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

import static org.hamcrest.CoreMatchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Comment management scenarios")
class CommentManagementTest extends IntegrationTestBase {

  @Autowired
  private AdRepository adRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User seller;
  private User commenter;
  private Category electronics;
  private Role roleUser;
  private Ad testAd;
  private String commenterToken;

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

    commenter = new User();
    commenter.setUsername("Комментатор");
    commenter.setEmail("commenter@test.com");
    commenter.setPasswordHash(passwordEncoder.encode("password123"));
    commenter.setRoles(Set.of(roleUser));
    userRepository.save(commenter);

    commenterToken = loginAndGetToken("commenter@test.com", "password123");

    testAd = new Ad();
    testAd.setTitle("Товар для комментариев");
    testAd.setDescription("Здесь будут тестировать комментарии");
    testAd.setCategory(electronics);
    testAd.setSeller(seller);
    testAd.setPrice(new BigDecimal("199.99"));
    adRepository.save(testAd);
    testAd.setStatus(AdStatus.ACTIVE);
    adRepository.save(testAd);

    Comment root1 = createComment(testAd, commenter, "Первый комментарий", null);
    Comment root2 = createComment(testAd, seller, "Ответ продавца", null);

    createComment(testAd, commenter, "Ответ на первый #1", root1);
    Comment reply1_2 = createComment(testAd, seller, "Ответ на первый #2", root1);
    createComment(testAd, commenter, "Вложенный ответ", reply1_2);
    createComment(testAd, commenter, "Ещё один ответ", root1);
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

  private Comment createComment(Ad ad, User author, String text, Comment parent) {
    Comment comment = new Comment();
    comment.setAd(ad);
    comment.setAuthor(author);
    comment.setText(text);
    comment.setParentComment(parent);
    commentRepository.save(comment);
    return comment;
  }

  @Test
  @DisplayName("Should return paginated root comments with FULL nested tree (any depth)")
  void getComments_withPagination_returnsRootCommentsWithFullTree() throws Exception {
    mockMvc.perform(get("/api/ads/{adId}/comments", testAd.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + commenterToken)
            .param("page", "0")
            .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.content[?(@.text == 'Первый комментарий')].replies.length()").value(3))
        .andExpect(jsonPath("$.content[?(@.text == 'Первый комментарий')].replies[?(@.text == 'Ответ на первый #2')].replies.length()").value(1))
        .andExpect(jsonPath("$.content[?(@.text == 'Первый комментарий')].replies[?(@.text == 'Ответ на первый #2')].replies[0].text").value("Вложенный ответ"))
        .andExpect(jsonPath("$.content[?(@.text == 'Ответ продавца')].replies.length()").value(0))
        .andExpect(jsonPath("$.content[*].author.username").exists())
        .andExpect(jsonPath("$.content[*].createdAt").exists());
  }

  @Test
  @DisplayName("Should create root comment when parentCommentId is null")
  void addComment_rootComment_success_createsAndReturnsDto() throws Exception {
    CommentRequestDto request = new CommentRequestDto();
    request.setText("Новый корневой комментарий");

    String response = mockMvc.perform(post("/api/ads/{adId}/comments", testAd.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + commenterToken)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.text").value("Новый корневой комментарий"))
        .andExpect(jsonPath("$.author.username").value("Комментатор"))
        .andExpect(jsonPath("$.replies").isEmpty())
        .andReturn().getResponse().getContentAsString();

    Long commentId = objectMapper.readTree(response).get("id").asLong();

    Comment saved = commentRepository.findByIdWithAuthor(commentId).orElseThrow();
    assertThat(saved.getText()).isEqualTo("Новый корневой комментарий");
    assertThat(saved.getAd().getId()).isEqualTo(testAd.getId());
    assertThat(saved.getAuthor().getId()).isEqualTo(commenter.getId());
    assertThat(saved.getParentComment()).isNull();
  }

  @Test
  @DisplayName("Should create reply when parentCommentId is provided and valid")
  void addComment_reply_success_createsNestedComment() throws Exception {
    CommentRequestDto rootRequest = new CommentRequestDto();
    rootRequest.setText("Корневой для ответа");

    String rootResponse = mockMvc.perform(post("/api/ads/{adId}/comments", testAd.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + commenterToken)
            .content(objectMapper.writeValueAsString(rootRequest)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long parentCommentId = objectMapper.readTree(rootResponse).get("id").asLong();

    CommentRequestDto replyRequest = new CommentRequestDto();
    replyRequest.setText("Это ответ на корневой");
    replyRequest.setParentCommentId(parentCommentId);

    mockMvc.perform(post("/api/ads/{adId}/comments", testAd.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + commenterToken)
            .content(objectMapper.writeValueAsString(replyRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.text").value("Это ответ на корневой"))
        .andExpect(jsonPath("$.replies").isEmpty());

    var replies = commentRepository.findRepliesByParentId(parentCommentId);
    assertThat(replies).hasSize(1);
    assertThat(replies.get(0).getText()).isEqualTo("Это ответ на корневой");
    assertThat(replies.get(0).getAd().getId()).isEqualTo(testAd.getId());
  }

  @Test
  @DisplayName("Should delete comment when author requests deletion")
  void deleteComment_author_success_removesComment() throws Exception {
    CommentRequestDto request = new CommentRequestDto();
    request.setText("Комментарий для удаления");

    String createResponse = mockMvc.perform(post("/api/ads/{adId}/comments", testAd.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + commenterToken)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long commentId = objectMapper.readTree(createResponse).get("id").asLong();

    mockMvc.perform(delete("/api/ads/{adId}/comments/{commentId}", testAd.getId(), commentId)
            .header("Authorization", "Bearer " + commenterToken))
        .andExpect(status().isNoContent());

    assertThat(commentRepository.findByIdWithAuthor(commentId)).isEmpty();

    mockMvc.perform(get("/api/ads/{adId}/comments", testAd.getId())
            .header("Authorization", "Bearer " + commenterToken)
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$..text").value(not(hasItem("Комментарий для удаления"))));
  }
}