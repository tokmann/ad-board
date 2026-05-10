package com.adboard.repository.impl;

import com.adboard.entity.Conversation;
import com.adboard.repository.ConversationRepository;
import com.adboard.repository.queries.ConversationQueries;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConversationRepositoryImpl implements ConversationRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Optional<Conversation> findByAdIdAndUserIds(Long adId, Long userId1, Long userId2) {
    try {
      Conversation conv = entityManager.createQuery(ConversationQueries.FIND_BY_AD_AND_USERS, Conversation.class)
          .setParameter("adId", adId)
          .setParameter("user1Id", userId1)
          .setParameter("user2Id", userId2)
          .getSingleResult();
      return Optional.of(conv);
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean isParticipant(Long conversationId, Long userId) {
    Long count = entityManager.createQuery(ConversationQueries.CHECK_PARTICIPANT, Long.class)
        .setParameter("convId", conversationId)
        .setParameter("userId", userId)
        .getSingleResult();
    return count > 0;
  }

  @Override
  public void save(Conversation conversation) {
    if (conversation.getId() == null) {
      entityManager.persist(conversation);
    } else {
      entityManager.merge(conversation);
    }
  }

  @Override
  public long count() {
    return entityManager.createQuery(ConversationQueries.COUNT_ALL_CONVERSATIONS, Long.class).getSingleResult();
  }
}
