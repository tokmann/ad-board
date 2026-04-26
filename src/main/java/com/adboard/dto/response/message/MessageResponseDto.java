package com.adboard.dto.response.message;

import com.adboard.dto.response.UserPreviewDto;
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