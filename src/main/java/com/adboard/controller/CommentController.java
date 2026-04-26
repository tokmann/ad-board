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
      @Parameter(description = "Ad ID") @PathVariable Long adId,
      @Parameter(description = "Page number (starting from 0)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
    log.info("Fetching comments for ad: {}, page={}, size={}", adId, page, size);
    PageResponse<CommentResponseDto> result = commentService.getCommentsByAdId(adId, page, size);
    log.info("Fetched {} comments for ad {}", result.getContent().size(), adId);
    return ResponseEntity.ok(result);
  }

  @Operation(summary = "Add a comment or reply to a comment")
  @PostMapping
  public ResponseEntity<CommentResponseDto> addComment(
      @Parameter(description = "Ad ID") @PathVariable Long adId,
      @Valid @RequestBody CommentRequestDto request,
      Authentication authentication) {
    log.info("Adding comment to ad: {} by user: {}", adId, authentication.getName());
    CommentResponseDto created = commentService.addComment(adId, request, authentication);
    log.info("Comment added successfully: id={}, isReply={}", created.getId(), request.getParentCommentId() != null);
    return ResponseEntity.status(201).body(created);
  }

  @Operation(summary = "Delete comment (available to author or administrator)")
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @Parameter(description = "Ad ID") @PathVariable Long adId,
      @Parameter(description = "Comment ID") @PathVariable Long commentId,
      Authentication authentication) {
    log.info("Deleting comment: {} from ad: {} by user: {}", commentId, adId, authentication.getName());
    commentService.deleteComment(adId, commentId, authentication);
    log.info("Comment deleted successfully: {}", commentId);
    return ResponseEntity.noContent().build();
  }
}
