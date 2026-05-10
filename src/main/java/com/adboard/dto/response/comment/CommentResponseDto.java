package com.adboard.dto.response.comment;

import com.adboard.dto.response.user.UserPreviewDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentResponseDto {

  private Long id;
  private String text;
  private UserPreviewDto author;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  private LocalDateTime createdAt;
  private List<CommentResponseDto> replies;
}