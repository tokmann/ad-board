package com.adboard.dto.response.comment;

import com.adboard.dto.response.UserPreviewDto;
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