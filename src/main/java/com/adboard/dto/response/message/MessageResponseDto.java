package com.adboard.dto.response.message;

import com.adboard.dto.response.user.UserPreviewDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageResponseDto {

  private Long id;
  private String content;
  private UserPreviewDto sender;
  private boolean read;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  private LocalDateTime createdAt;
}