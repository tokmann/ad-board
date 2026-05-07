package com.adboard.repository.impl;

import com.adboard.entity.Ad;
import com.adboard.entity.enums.AdStatus;
import com.adboard.repository.AdRepository;
import com.adboard.repository.queries.AdQueries;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AdRepositoryImpl implements AdRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Optional<Ad> findById(Long id) {
    try {
      return Optional.of(entityManager.createQuery(AdQueries.FIND_BY_ID, Ad.class)
          .setParameter("id", id)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  public void save(Ad ad) {
    if (ad.getId() == null) {
      entityManager.persist(ad);
    } else {
      entityManager.merge(ad);
    }
  }

  @Override
  public Optional<Ad> findActiveById(Long id) {
    try {
      return Optional.of(entityManager.createQuery(AdQueries.FIND_ACTIVE_BY_ID, Ad.class)
          .setParameter("id", id)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  public void delete(Ad ad) {
    entityManager.remove(entityManager.contains(ad) ? ad : entityManager.merge(ad));
  }

  @Override
  public List<Ad> search(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, AdStatus status, int page, int size) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Ad> query = cb.createQuery(Ad.class);
    Root<Ad> root = query.from(Ad.class);
    root.fetch("seller", JoinType.LEFT);
    root.fetch("category", JoinType.LEFT);

    List<Predicate> predicates = new ArrayList<>();

    if (status != null) {
      predicates.add(cb.equal(root.get("status"), status));
    } else {
      predicates.add(cb.equal(root.get("status"), AdStatus.ACTIVE));
    }

    if (keyword != null && !keyword.isBlank()) {
      String pattern = "%" + keyword.toLowerCase() + "%";
      predicates.add(cb.or(
          cb.like(cb.lower(root.get("title")), pattern),
          cb.like(cb.lower(root.get("description")), pattern)
      ));
    }

    if (categoryId != null) {
      predicates.add(cb.equal(root.get("category").get("id"), categoryId));
    }

    if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
    if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));

    query.where(predicates.toArray(new Predicate[0]));
    query.orderBy(cb.desc(root.get("isPromoted")), cb.desc(root.get("createdAt")));

    return entityManager.createQuery(query)
        .setFirstResult(page * size)
        .setMaxResults(size)
        .getResultList();
  }

  @Override
  public long countSearch(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, AdStatus status) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> query = cb.createQuery(Long.class);
    Root<Ad> root = query.from(Ad.class);
    query.select(cb.count(root));

    List<Predicate> predicates = new ArrayList<>();

    if (status != null) {
      predicates.add(cb.equal(root.get("status"), status));
    } else {
      predicates.add(cb.equal(root.get("status"), AdStatus.ACTIVE));
    }

    if (keyword != null && !keyword.isBlank()) {
      String pattern = "%" + keyword.toLowerCase() + "%";
      predicates.add(cb.or(
          cb.like(cb.lower(root.get("title")), pattern),
          cb.like(cb.lower(root.get("description")), pattern)
      ));
    }

    if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
    if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
    if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));

    query.where(predicates.toArray(new Predicate[0]));
    return entityManager.createQuery(query).getSingleResult();
  }

  @Override
  public List<Ad> findSoldBySellerEmail(String sellerEmail, int page, int size) {
    return entityManager.createQuery(AdQueries.FIND_SOLD_BY_SELLER_EMAIL, Ad.class)
        .setParameter("sellerEmail", sellerEmail)
        .setFirstResult(page * size)
        .setMaxResults(size)
        .getResultList();
  }

  @Override
  public long countSoldBySellerEmail(String sellerEmail) {
    return entityManager.createQuery(AdQueries.COUNT_SOLD_BY_SELLER_EMAIL, Long.class)
        .setParameter("sellerEmail", sellerEmail)
        .getSingleResult();
  }
}
