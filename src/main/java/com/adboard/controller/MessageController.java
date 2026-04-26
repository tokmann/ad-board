package com.adboard.controller;

import com.adboard.dto.request.message.MessageRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.message.MessageResponseDto;
import com.adboard.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ads/{adId}/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Personal chat regarding the ad")
public class MessageController {

  private final MessageService messageService;

  @Operation(summary = "Get the chat history with a specific user")
  @GetMapping
  public ResponseEntity<PageResponse<MessageResponseDto>> getMessages(
      @Parameter(description = "Ad ID") @PathVariable Long adId,
      @Parameter(description = "Companion ID (required)") @RequestParam Long withUserId,
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      Authentication authentication) {
    log.info("Fetching chat: ad={}, withUser={}, myUser={}", adId, withUserId, authentication.getName());
    PageResponse<MessageResponseDto> result = messageService.getConversationMessages(adId, withUserId, page, size, authentication);
    return ResponseEntity.ok(result);
  }

  @Operation(summary = "Send message (creates a chat if there isn't one)")
  @PostMapping
  public ResponseEntity<MessageResponseDto> sendMessage(
      @Parameter(description = "Ad ID") @PathVariable Long adId,
      @Valid @RequestBody MessageRequestDto request,
      Authentication authentication) {
    log.info("Sending message in ad: {}, to user: {}", adId, request.getReceiverId());
    MessageResponseDto sentMessage = messageService.sendMessage(adId, request, authentication);
    log.info("Message sent successfully: id={}", sentMessage.getId());
    return ResponseEntity.status(201).body(sentMessage);
  }
}
