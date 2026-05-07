package com.adboard.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequestDto {

  @NotBlank(message = "Comment text is required")
  @Size(max = 2000, message = "Comment text is too long")
  private String text;

  private Long parentCommentId;
}