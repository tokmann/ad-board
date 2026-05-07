package com.adboard.service;

import com.adboard.dto.mapper.CommentMapper;
import com.adboard.dto.request.comment.CommentRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.comment.CommentResponseDto;
import com.adboard.entity.Ad;
import com.adboard.entity.Comment;
import com.adboard.entity.User;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.exception.UnauthorizedActionException;
import com.adboard.repository.AdRepository;
import com.adboard.repository.CommentRepository;
import com.adboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final AdRepository adRepository;
  private final UserRepository userRepository;
  private final CommentMapper commentMapper;

  @Transactional(readOnly = true)
  public PageResponse<CommentResponseDto> getCommentsByAdId(Long adId, int page, int size) {
    if (adRepository.findById(adId).isEmpty()) {
      throw new ResourceNotFoundException("Ad not found with id: " + adId);
    }

    long totalElements = commentRepository.countRootByAdId(adId);
    List<Comment> comments = commentRepository.findRootByAdId(adId, page, size);

    List<CommentResponseDto> content = comments.stream()
        .map(commentMapper::toResponseDto)
        .peek(dto -> {
          List<Comment> replies = commentRepository.findRepliesByParentId(dto.getId());
          dto.setReplies(replies.stream().map(commentMapper::toResponseDto).toList());
        })
        .toList();

    int totalPages = (int) Math.ceil((double) totalElements / size);

    log.info("Successfully retrieved {} comments for ad {} (page {}/{})",
        content.size(), adId, page + 1, totalPages);

    return new PageResponse<>(content, page, size, totalElements, totalPages);
  }

  @Transactional
  public CommentResponseDto addComment(Long adId, CommentRequestDto request, Authentication authentication) {
    String authorEmail = authentication.getName();

    Ad ad = adRepository.findById(adId).orElseThrow(() -> new ResourceNotFoundException("Ad not found with id: " + adId));

    User author = userRepository.findByEmail(authorEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + authorEmail));

    Comment parentComment = null;
    if (request.getParentCommentId() != null) {
      parentComment = commentRepository.findByIdWithAuthor(request.getParentCommentId())
          .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + request.getParentCommentId()));

      if (!parentComment.getAd().getId().equals(adId)) {
        throw new IllegalArgumentException("Parent comment does not belong to this ad");
      }
    }

    Comment comment = commentMapper.toEntity(request);
    comment.setAd(ad);
    comment.setAuthor(author);
    comment.setParentComment(parentComment);

    commentRepository.save(comment);

    log.info("Comment added successfully: id={}, ad={}, by={}",
        comment.getId(), adId, authorEmail);

    return commentMapper.toResponseDto(comment);
  }

  @Transactional
  public void deleteComment(Long adId, Long commentId, Authentication authentication) {
    String currentUserEmail = authentication.getName();

    Comment comment = commentRepository.findByIdWithAuthor(commentId)
        .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

    if (!comment.getAd().getId().equals(adId)) {
      throw new IllegalArgumentException("Comment does not belong to this ad");
    }

    if (!comment.getAuthor().getEmail().equals(currentUserEmail)) {
      throw new UnauthorizedActionException("You are not the author of this comment");
    }

    commentRepository.delete(comment);

    log.info("Comment deleted successfully: id={}, ad={}", commentId, adId);
  }
}
