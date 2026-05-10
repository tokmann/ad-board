package com.adboard.repository.impl;

import com.adboard.entity.Message;
import com.adboard.repository.MessageRepository;
import com.adboard.repository.queries.MessageQueries;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MessageRepositoryImpl implements MessageRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public List<Message> findByConversationId(Long conversationId, int page, int size) {
    return entityManager.createQuery(MessageQueries.FIND_BY_CONVERSATION, Message.class)
        .setParameter("convId", conversationId)
        .setFirstResult(page * size)
        .setMaxResults(size)
        .getResultList();
  }

  @Override
  public long countByConversationId(Long conversationId) {
    return entityManager.createQuery(MessageQueries.COUNT_BY_CONVERSATION, Long.class)
        .setParameter("convId", conversationId)
        .getSingleResult();
  }

  @Override
  public void save(Message message) {
    if (message.getId() == null) {
      entityManager.persist(message);
    } else {
      entityManager.merge(message);
    }
  }

  @Override
  public Optional<Message> findById(Long id) {
    try {
      return Optional.of(entityManager.createQuery(MessageQueries.FIND_MESSAGE_BY_ID, Message.class)
          .setParameter("id", id)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
}
