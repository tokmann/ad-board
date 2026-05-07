package com.adboard.repository;

import com.adboard.entity.Message;

import java.util.List;

public interface MessageRepository {

  List<Message> findByConversationId(Long conversationId, int page, int size);
  long countByConversationId(Long conversationId);
  void save(Message message);
}
