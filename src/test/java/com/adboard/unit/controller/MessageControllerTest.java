package com.adboard.unit.controller;

import com.adboard.controller.MessageController;
import com.adboard.dto.request.message.MessageRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.message.MessageResponseDto;
import com.adboard.exception.ConversationBlockedException;
import com.adboard.exception.GlobalExceptionHandler;
import com.adboard.service.MessageService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

  @Mock
  private MessageService messageService;

  @Mock
  private Authentication auth;

  @InjectMocks
  private MessageController messageController;

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

    mockMvc = MockMvcBuilders.standaloneSetup(messageController)
        .setCustomArgumentResolvers(authResolver)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  /**
   * Testing getMessages
   * */

  @Test
  @DisplayName("Should return chat history when withUserId is provided")
  void getMessages_success() throws Exception {
    MessageResponseDto messageDto = new MessageResponseDto();
    messageDto.setId(100L);
    messageDto.setContent("Is the item still available?");

    PageResponse<MessageResponseDto> pageResponse = new PageResponse<>(
        List.of(messageDto), 0, 20, 1, 1
    );

    when(messageService.getConversationMessages(eq(1L), eq(2L), eq(0), eq(20), any()))
        .thenReturn(pageResponse);

    mockMvc.perform(get("/api/ads/1/messages")
            .param("withUserId", "2")
            .param("page", "0")
            .param("size", "20")
            .principal(auth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].content").value("Is the item still available?"));
  }

  @Test
  @DisplayName("Should return 400 when required param withUserId is missing")
  void getMessages_missingWithUserId_returns400() throws Exception {
    mockMvc.perform(get("/api/ads/1/messages")
            .principal(auth))
        .andExpect(status().isBadRequest());
  }

  /**
   * Testing sendMessage
   * */

  @Test
  @DisplayName("Should send a new message successfully")
  void sendMessage_success() throws Exception {
    MessageRequestDto request = new MessageRequestDto();
    request.setContent("Yes, it is.");
    request.setReceiverId(2L);

    MessageResponseDto response = new MessageResponseDto();
    response.setId(101L);
    response.setContent("Yes, it is.");

    when(messageService.sendMessage(eq(1L), any(MessageRequestDto.class), any()))
        .thenReturn(response);

    mockMvc.perform(post("/api/ads/1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(101L))
        .andExpect(jsonPath("$.content").value("Yes, it is."));
  }

  @Test
  @DisplayName("Should return 400 when message content is blank")
  void sendMessage_blankContent_returns400() throws Exception {
    MessageRequestDto request = new MessageRequestDto();
    request.setContent("");
    request.setReceiverId(2L);

    mockMvc.perform(post("/api/ads/1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when sending message to yourself")
  void sendMessage_toSelf_returns400() throws Exception {
    MessageRequestDto request = new MessageRequestDto();
    request.setContent("Some content");
    request.setReceiverId(1L);

    when(messageService.sendMessage(eq(1L), any(MessageRequestDto.class), any()))
        .thenThrow(new IllegalArgumentException("Cannot send a message to yourself"));

    mockMvc.perform(post("/api/ads/1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Cannot send a message to yourself"));
  }

  @Test
  @DisplayName("Should return 409 when conversation is blocked")
  void sendMessage_conversationBlocked_returns409() throws Exception {
    MessageRequestDto request = new MessageRequestDto();
    request.setContent("Some content");
    request.setReceiverId(2L);

    when(messageService.sendMessage(eq(1L), any(MessageRequestDto.class), any()))
        .thenThrow(new ConversationBlockedException("Cannot send messages: conversation is blocked"));

    mockMvc.perform(post("/api/ads/1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .principal(auth))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Cannot send messages: conversation is blocked"));
  }
}
