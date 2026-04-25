package com.adboard.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentResponseDto {
  private Long id;
  private String text;
  private UserPreviewDto author;
  private LocalDateTime createdAt;
  private List<CommentResponseDto> replies;
}