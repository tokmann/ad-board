package com.adboard.dto.response.review;

import com.adboard.dto.response.user.UserPreviewDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponseDto {

  private Long id;
  private Integer rating;
  private String commentText;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  private LocalDateTime createdAt;
  private UserPreviewDto reviewer;
}