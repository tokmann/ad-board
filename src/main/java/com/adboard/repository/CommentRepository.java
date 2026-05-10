package com.adboard.repository;

import com.adboard.entity.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {

  List<Comment> findRootByAdId(Long adId, int page, int size);
  long countRootByAdId(Long adId);
  Optional<Comment> findByIdWithAuthor(Long id);
  List<Comment> findRepliesByParentId(Long parentId);
  List<Comment> findAllByAdId(Long adId);
  void save(Comment comment);
  void delete(Comment comment);
}
