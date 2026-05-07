package com.adboard.repository.impl;

import com.adboard.entity.Comment;
import com.adboard.repository.CommentRepository;
import com.adboard.repository.queries.CommentQueries;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CommentRepositoryImpl implements CommentRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public List<Comment> findRootByAdId(Long adId, int page, int size) {
    return entityManager.createQuery(CommentQueries.FIND_ROOT_BY_AD_ID, Comment.class)
        .setParameter("adId", adId)
        .setFirstResult(page * size)
        .setMaxResults(size)
        .getResultList();
  }

  @Override
  public long countRootByAdId(Long adId) {
    return entityManager.createQuery(CommentQueries.COUNT_ROOT_BY_AD_ID, Long.class)
        .setParameter("adId", adId)
        .getSingleResult();
  }

  @Override
  public Optional<Comment> findByIdWithAuthor(Long id) {
    try {
      return Optional.of(entityManager.createQuery(CommentQueries.FIND_BY_ID_WITH_AUTHOR, Comment.class)
          .setParameter("id", id)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  public List<Comment> findRepliesByParentId(Long parentId) {
    return entityManager.createQuery(CommentQueries.FIND_REPLIES_BY_PARENT_ID, Comment.class)
        .setParameter("parentId", parentId)
        .getResultList();
  }

  @Override
  public void save(Comment comment) {
    if (comment.getId() == null) {
      entityManager.persist(comment);
    } else {
      entityManager.merge(comment);
    }
  }

  @Override
  public void delete(Comment comment) {
    entityManager.remove(entityManager.contains(comment) ? comment : entityManager.merge(comment));
  }
}
