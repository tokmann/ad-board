package com.adboard.service;

import com.adboard.dto.mapper.MessageMapper;
import com.adboard.dto.request.message.MessageRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.message.MessageResponseDto;
import com.adboard.entity.Ad;
import com.adboard.entity.Conversation;
import com.adboard.entity.Message;
import com.adboard.entity.User;
import com.adboard.entity.enums.ConversationStatus;
import com.adboard.exception.ConversationBlockedException;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.exception.UnauthorizedActionException;
import com.adboard.repository.AdRepository;
import com.adboard.repository.ConversationRepository;
import com.adboard.repository.MessageRepository;
import com.adboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

  private final AdRepository adRepository;
  private final UserRepository userRepository;
  private final ConversationRepository conversationRepository;
  private final MessageRepository messageRepository;
  private final MessageMapper messageMapper;

  @Transactional(readOnly = true)
  public PageResponse<MessageResponseDto> getConversationMessages(Long adId,
                                                                  Long withUserId,
                                                                  int page,
                                                                  int size,
                                                                  Authentication authentication) {
    String currentEmail = authentication.getName();

    User currentUser = userRepository.findByEmail(currentEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentEmail));

    if (userRepository.findById(withUserId).isEmpty()) {
      throw new ResourceNotFoundException("User not found with id: " + withUserId);
    }

    if (adRepository.findById(adId).isEmpty()) {
      throw new ResourceNotFoundException("Ad not found with id: " + adId);
    }

    Conversation conversation = findOrCreateConversation(adId, currentUser.getId(), withUserId);

    if (!conversationRepository.isParticipant(conversation.getId(), currentUser.getId())) {
      throw new UnauthorizedActionException("Access denied to this conversation");
    }

    long totalElements = messageRepository.countByConversationId(conversation.getId());
    List<Message> messages = messageRepository.findByConversationId(conversation.getId(), page, size);

    List<MessageResponseDto> content = messages.stream()
        .map(messageMapper::toResponseDto)
        .toList();

    int totalPages = (int) Math.ceil((double) totalElements / size);

    log.info("Successfully retrieved {} messages for conversation: ad={}, withUser={}",
        content.size(), adId, withUserId);

    return new PageResponse<>(content, page, size, totalElements, totalPages);
  }

  @Transactional
  public MessageResponseDto sendMessage(Long adId, MessageRequestDto request, Authentication authentication) {
    String senderEmail = authentication.getName();

    User sender = userRepository.findByEmail(senderEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + senderEmail));

    Ad ad = adRepository.findById(adId)
        .orElseThrow(() -> new ResourceNotFoundException("Ad not found with id: " + adId));

    User receiver;
    if (request.getReceiverId() != null) {
      receiver = userRepository.findById(request.getReceiverId())
          .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getReceiverId()));
    } else {
      receiver = ad.getSeller();
    }

    if (sender.getId().equals(receiver.getId())) {
      throw new IllegalArgumentException("Cannot send a message to yourself");
    }

    Conversation conversation = findOrCreateConversation(adId, sender.getId(), receiver.getId());

    if (conversation.getStatus() == ConversationStatus.BLOCKED) {
      throw new ConversationBlockedException("Cannot send messages: conversation is blocked");
    }

    Message message = messageMapper.toEntity(request);
    message.setConversation(conversation);
    message.setSender(sender);

    messageRepository.save(message);

    conversation.setUpdatedAt(LocalDateTime.now());
    conversationRepository.save(conversation);

    log.info("Message sent successfully: conversation={}, from={}, to={}",
        conversation.getId(), senderEmail, receiver.getEmail());

    return messageMapper.toResponseDto(message);
  }

  private Conversation findOrCreateConversation(Long adId, Long userId1, Long userId2) {
    return conversationRepository.findByAdIdAndUserIds(adId, userId1, userId2)
        .orElseGet(() -> {
          Conversation conv = new Conversation();
          conv.setAd(adRepository.findById(adId)
              .orElseThrow(() -> new ResourceNotFoundException("Ad not found: " + adId)));

          Ad ad = conv.getAd();
          if (ad.getSeller().getId().equals(userId1)) {
            conv.setSeller(ad.getSeller());
            conv.setBuyer(userRepository.findById(userId2)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId2)));
          } else {
            conv.setSeller(ad.getSeller());
            conv.setBuyer(userRepository.findById(userId1)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId1)));
          }

          conv.setStatus(ConversationStatus.ACTIVE);
          conv.setCreatedAt(LocalDateTime.now());
          conv.setUpdatedAt(LocalDateTime.now());

          conversationRepository.save(conv);
          log.debug("New conversation created: ad={}, users={}/{}", adId, userId1, userId2);
          return conv;
        });
  }
}
