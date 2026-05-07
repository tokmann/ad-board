package com.adboard.repository;

import com.adboard.entity.Ad;
import com.adboard.entity.enums.AdStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AdRepository {

  Optional<Ad> findById(Long id);
  void save(Ad ad);
  Optional<Ad> findActiveById(Long id);
  void delete(Ad ad);
  List<Ad> search(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, AdStatus status, int page, int size);
  long countSearch(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, AdStatus status);
  List<Ad> findSoldBySellerEmail(String sellerEmail, int page, int size);
  long countSoldBySellerEmail(String sellerEmail);
}
