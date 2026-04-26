package com.adboard.service;

import com.adboard.dto.request.comment.CommentRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.comment.CommentResponseDto;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

  public PageResponse<CommentResponseDto> getCommentsByAdId(Long adId, int page, int size) {
    return null;
  }

  public CommentResponseDto addComment(Long adId, CommentRequestDto request, Authentication authentication) {
    return null;
  }

  public void deleteComment(Long adId, Long commentId, Authentication authentication) {
  }
}
