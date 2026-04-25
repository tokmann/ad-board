package com.adboard.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageResponseDto {
  private Long id;
  private String content;
  private UserPreviewDto sender;
  private boolean isRead;
  private LocalDateTime createdAt;
}