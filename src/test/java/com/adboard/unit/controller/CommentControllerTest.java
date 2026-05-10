package com.adboard.unit.controller;

import com.adboard.controller.CommentController;
import com.adboard.dto.request.comment.CommentRequestDto;
import com.adboard.dto.response.comment.CommentResponseDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.exception.GlobalExceptionHandler;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.exception.UnauthorizedActionException;
import com.adboard.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

  @Mock
  private CommentService commentService;

  @Mock
  private Authentication auth;

  @InjectMocks
  private CommentController commentController;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    when(auth.getName()).thenReturn("test@test.com");

    HandlerMethodArgumentResolver authResolver = new HandlerMethodArgumentResolver() {

      @Override
      public boolean supportsParameter(MethodParameter parameter) {
        return Authentication.class.isAssignableFrom(parameter.getParameterType());
      }

      @Override
      public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return auth;
      }
    };

    mockMvc = MockMvcBuilders.standaloneSetup(commentController)
        .setCustomArgumentResolvers(authResolver)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  /**
   * Testing getComments
   * */

  @Test
  @DisplayName("Should return page of comments for an ad")
  void getComments_success() throws Exception {
    CommentResponseDto dto = new CommentResponseDto();
    dto.setId(1L);
    dto.setText("Отличное объявление!");

    PageResponse<CommentResponseDto> response = new PageResponse<>(
        List.of(dto), 0, 10, 1, 1
    );

    when(commentService.getCommentsByAdId(eq(1L), anyInt(), anyInt())).thenReturn(response);

    mockMvc.perform(get("/api/ads/1/comments")
            .param("page", "0")
            .param("size", "10")
            .principal(auth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].text").value("Отличное объявление!"));
  }

  @Test
  @DisplayName("Should return 404 when ad not found during fetch")
  void getComments_adNotFound_returns404() throws Exception {
    when(commentService.getCommentsByAdId(eq(999L), anyInt(), anyInt()))
        .thenThrow(new ResourceNotFoundException("Ad not found"));

    mockMvc.perform(get("/api/ads/999/comments")
            .principal(auth))
        .andExpect(status().isNotFound());
  }

  /**
   * Testing addComment
   * */

  @Test
  @DisplayName("Should create a new comment")
  void addComment_success() throws Exception {
    CommentRequestDto request = new CommentRequestDto();
    request.setText("Мой новый комментарий");

    CommentResponseDto response = new CommentResponseDto();
    response.setId(10L);
    response.setText("Мой новый комментарий");

    when(commentService.addComment(eq(1L), any(), any())).thenReturn(response);

    mockMvc.perform(post("/api/ads/1/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(10L));
  }

  @Test
  @DisplayName("Should return 400 when comment text is empty")
  void addComment_invalidRequest_returns400() throws Exception {
    CommentRequestDto invalidRequest = new CommentRequestDto();
    invalidRequest.setText("");

    mockMvc.perform(post("/api/ads/1/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
            .principal(auth))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 404 when parent comment not found")
  void addComment_parentNotFound_returns404() throws Exception {
    CommentRequestDto request = new CommentRequestDto();
    request.setText("Текст ответа");
    request.setParentCommentId(555L);

    when(commentService.addComment(eq(1L), any(), any()))
        .thenThrow(new ResourceNotFoundException("Comment not found"));

    mockMvc.perform(post("/api/ads/1/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 when parent comment belongs to different ad")
  void addComment_wrongAdParent_returns400() throws Exception {
    CommentRequestDto request = new CommentRequestDto();
    request.setText("Текст ответа");
    request.setParentCommentId(555L);

    when(commentService.addComment(eq(1L), any(), any()))
        .thenThrow(new IllegalArgumentException("Parent comment does not belong to this ad"));

    mockMvc.perform(post("/api/ads/1/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Parent comment does not belong to this ad"));
  }

  /**
   * Testing deleteComment
   * */

  @Test
  @DisplayName("Should return 204 when comment is deleted by author")
  void deleteComment_success() throws Exception {
    mockMvc.perform(delete("/api/ads/1/comments/10")
            .principal(auth))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should return 403 when trying to delete someone else's comment")
  void deleteComment_unauthorized_returns403() throws Exception {
    doThrow(new UnauthorizedActionException("Not your comment"))
        .when(commentService).deleteComment(eq(1L), eq(10L), any());

    mockMvc.perform(delete("/api/ads/1/comments/10")
            .principal(auth))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Not your comment"));
  }

  @Test
  @DisplayName("Should return 404 when deleting non-existent comment")
  void deleteComment_notFound_returns404() throws Exception {
    doThrow(new ResourceNotFoundException("Comment not found"))
        .when(commentService).deleteComment(eq(1L), eq(999L), any());

    mockMvc.perform(delete("/api/ads/1/comments/999")
            .principal(auth))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 when comment does not belong to the ad in path")
  void deleteComment_wrongAd_returns400() throws Exception {
    doThrow(new IllegalArgumentException("Comment does not belong to this ad"))
        .when(commentService).deleteComment(eq(1L), eq(10L), any());

    mockMvc.perform(delete("/api/ads/1/comments/10")
            .principal(auth))
        .andExpect(status().isBadRequest());
  }
}