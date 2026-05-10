package com.adboard.repository;

import com.adboard.entity.Message;

import java.util.List;
import java.util.Optional;

public interface MessageRepository {

  List<Message> findByConversationId(Long conversationId, int page, int size);
  long countByConversationId(Long conversationId);
  void save(Message message);
  Optional<Message> findById(Long id);
}
