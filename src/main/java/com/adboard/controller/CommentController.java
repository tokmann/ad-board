package com.adboard.controller;

import com.adboard.dto.request.comment.CommentRequestDto;
import com.adboard.dto.response.comment.CommentResponseDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ads/{adId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Managing comments and replies to ads")
public class CommentController {

  private final CommentService commentService;

  @Operation(summary = "Get a list of comments for an ad with pagination")
  @GetMapping
  public ResponseEntity<PageResponse<CommentResponseDto>> getComments(
      @Parameter(description = "Ad ID") @PathVariable("adId") Long adId,
      @Parameter(description = "Page number (starting from 0)") @RequestParam(name = "page", defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(name = "size", defaultValue = "10") int size) {
    log.info("REST request to fetch comments for ad: {}, page={}", adId, page);
    PageResponse<CommentResponseDto> result = commentService.getCommentsByAdId(adId, page, size);
    return ResponseEntity.ok(result);
  }

  @Operation(summary = "Add a comment or reply to a comment")
  @PostMapping
  public ResponseEntity<CommentResponseDto> addComment(
      @Parameter(description = "Ad ID") @PathVariable("adId") Long adId,
      @Valid @RequestBody CommentRequestDto request,
      Authentication authentication) {
    log.info("REST request to add comment to ad: {} by user: {}", adId, authentication.getName());
    CommentResponseDto created = commentService.addComment(adId, request, authentication);
    return ResponseEntity.status(201).body(created);
  }

  @Operation(summary = "Delete comment (Available to author or administrator)")
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @Parameter(description = "Ad ID") @PathVariable("adId") Long adId,
      @Parameter(description = "Comment ID") @PathVariable("commentId") Long commentId,
      Authentication authentication) {
    log.info("REST request to delete comment: {} for ad: {}", commentId, adId);
    commentService.deleteComment(adId, commentId, authentication);
    return ResponseEntity.noContent().build();
  }
}
