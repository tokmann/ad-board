package com.adboard.service;

import com.adboard.dto.request.message.MessageRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.message.MessageResponseDto;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
  public PageResponse<MessageResponseDto> getConversationMessages(Long adId,
                                                                  Long withUserId,
                                                                  int page,
                                                                  int size,
                                                                  Authentication authentication) {
    return null;
  }

  public MessageResponseDto sendMessage(Long adId, MessageRequestDto request, Authentication authentication) {
    return null;
  }
}
