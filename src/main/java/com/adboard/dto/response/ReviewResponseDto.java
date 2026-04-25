package com.adboard.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponseDto {
  private Long id;
  private Integer rating;
  private String commentText;
  private LocalDateTime createdAt;
  private UserPreviewDto reviewer;
}