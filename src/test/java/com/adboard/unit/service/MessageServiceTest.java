package com.adboard.unit.service;

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
import com.adboard.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @Mock
  private AdRepository adRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ConversationRepository conversationRepository;

  @Mock
  private MessageRepository messageRepository;

  @Mock
  private MessageMapper messageMapper;

  @InjectMocks
  private MessageService messageService;

  private Ad ad;
  private User seller;
  private User buyer;
  private Conversation conversation;
  private Message message;
  private MessageResponseDto messageDto;
  private MessageRequestDto request;
  private Authentication auth;

  @BeforeEach
  void setUp() {
    seller = new User();
    seller.setId(1L);
    seller.setEmail("seller@test.com");

    buyer = new User();
    buyer.setId(2L);
    buyer.setEmail("buyer@test.com");

    ad = new Ad();
    ad.setId(10L);
    ad.setSeller(seller);

    conversation = new Conversation();
    conversation.setId(100L);
    conversation.setAd(ad);
    conversation.setSeller(seller);
    conversation.setBuyer(buyer);
    conversation.setStatus(ConversationStatus.ACTIVE);

    message = new Message();
    message.setId(1000L);
    message.setContent("Привет!");
    message.setConversation(conversation);
    message.setSender(buyer);

    messageDto = new MessageResponseDto();
    messageDto.setId(1000L);
    messageDto.setContent("Привет!");

    request = new MessageRequestDto();
    request.setContent("Привет!");
    request.setReceiverId(seller.getId());

    auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("buyer@test.com");
  }

  @Test
  @DisplayName("Should return paginated messages for existing conversation")
  void getConversationMessages_returnsPaginatedMessages_whenConversationExists() {
    when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
    when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
    when(conversationRepository.findByAdIdAndUserIds(10L, 2L, 1L)).thenReturn(Optional.of(conversation));
    when(conversationRepository.isParticipant(100L, 2L)).thenReturn(true);
    when(messageRepository.countByConversationId(100L)).thenReturn(15L);
    when(messageRepository.findByConversationId(100L, 0, 10)).thenReturn(List.of(message));
    when(messageMapper.toResponseDto(message)).thenReturn(messageDto);

    PageResponse<MessageResponseDto> result = messageService.getConversationMessages(10L, 1L, 0, 10, auth);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(15);
    assertThat(result.getTotalPages()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should throw forbidden when user is not participant of conversation")
  void getConversationMessages_throws_whenUserNotParticipant() {
    when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
    when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
    when(conversationRepository.findByAdIdAndUserIds(10L, 2L, 1L)).thenReturn(Optional.of(conversation));
    when(conversationRepository.isParticipant(100L, 2L)).thenReturn(false);

    assertThatThrownBy(() -> messageService.getConversationMessages(10L, 1L, 0, 10, auth))
        .isInstanceOf(UnauthorizedActionException.class);
  }

  @Test
  @DisplayName("Should throw exception when ad is not found")
  void getConversationMessages_throws_whenAdNotFound() {
    when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
    when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
    when(adRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> messageService.getConversationMessages(999L, 1L, 0, 10, auth))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("Should create new conversation when sending first message")
  void sendMessage_createsNewConversation_whenFirstMessage() {
    when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
    when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
    when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
    when(conversationRepository.findByAdIdAndUserIds(10L, 2L, 1L)).thenReturn(Optional.empty());
    when(messageMapper.toEntity(request)).thenAnswer(inv -> {
      Message newMsg = new Message();
      newMsg.setContent(request.getContent());
      return newMsg;
    });
    when(messageMapper.toResponseDto(any(Message.class))).thenReturn(messageDto);

    messageService.sendMessage(10L, request, auth);

    ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
    verify(conversationRepository, times(2)).save(convCaptor.capture());
    Conversation created = convCaptor.getAllValues().get(0);

    assertThat(created.getAd()).isEqualTo(ad);
    assertThat(created.getSeller()).isEqualTo(seller);
    assertThat(created.getBuyer()).isEqualTo(buyer);
    assertThat(created.getStatus()).isEqualTo(ConversationStatus.ACTIVE);

    ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageRepository).save(msgCaptor.capture());
    assertThat(msgCaptor.getValue().getSender()).isEqualTo(buyer);
  }

  @Test
  @DisplayName("Should reuse existing conversation when sending follow-up message")
  void sendMessage_usesExistingConversation_whenAlreadyExists() {
    when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
    when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
    when(conversationRepository.findByAdIdAndUserIds(10L, 2L, 1L)).thenReturn(Optional.of(conversation));
    when(messageMapper.toEntity(request)).thenAnswer(inv -> {
      Message newMsg = new Message();
      newMsg.setContent(request.getContent());
      return newMsg;
    });
    when(messageMapper.toResponseDto(any(Message.class))).thenReturn(messageDto);

    messageService.sendMessage(10L, request, auth);

    verify(conversationRepository, times(1)).save(any(Conversation.class));
    ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageRepository).save(msgCaptor.capture());
    assertThat(msgCaptor.getValue().getConversation()).isEqualTo(conversation);
  }

  @Test
  @DisplayName("Should throw exception when user tries to send message to themselves")
  void sendMessage_throws_whenSendingToSelf() {
    request.setReceiverId(buyer.getId());

    when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
    when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));

    assertThatThrownBy(() -> messageService.sendMessage(10L, request, auth))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should throw exception when conversation is blocked")
  void sendMessage_throws_whenConversationBlocked() {
    conversation.setStatus(ConversationStatus.BLOCKED);

    when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
    when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
    when(conversationRepository.findByAdIdAndUserIds(10L, 2L, 1L)).thenReturn(Optional.of(conversation));

    assertThatThrownBy(() -> messageService.sendMessage(10L, request, auth))
        .isInstanceOf(ConversationBlockedException.class);
  }

  @Test
  @DisplayName("Should set seller as receiver when receiverId is not provided")
  void sendMessage_setsReceiverToSeller_whenReceiverIdNotProvided() {
    request.setReceiverId(null);

    when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
    when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
    when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
    when(conversationRepository.findByAdIdAndUserIds(10L, 2L, 1L)).thenReturn(Optional.empty());
    when(messageMapper.toEntity(request)).thenAnswer(inv -> {
      Message newMsg = new Message();
      newMsg.setContent(request.getContent());
      return newMsg;
    });
    when(messageMapper.toResponseDto(any(Message.class))).thenReturn(messageDto);

    messageService.sendMessage(10L, request, auth);

    ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
    verify(conversationRepository, times(2)).save(convCaptor.capture());
    Conversation updated = convCaptor.getAllValues().get(1);
    assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());
  }
}