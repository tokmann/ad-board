package com.adboard.repository;

import com.adboard.entity.Conversation;

import java.util.Optional;

public interface ConversationRepository {

  Optional<Conversation> findByAdIdAndUserIds(Long adId, Long userId1, Long userId2);
  boolean isParticipant(Long conversationId, Long userId);
  void save(Conversation conversation);
}
