package com.adboard.dto.response.ad;

import com.adboard.dto.response.category.CategoryDto;
import com.adboard.dto.response.user.UserPreviewDto;
import com.adboard.entity.enums.AdStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdResponseDto {

  private Long id;
  private String title;
  private String description;
  private BigDecimal price;
  private AdStatus status;
  private String imageUrl;
  private boolean isPromoted;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  private LocalDateTime promoteExpiresAt;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  private LocalDateTime createdAt;
  private UserPreviewDto seller;
  private UserPreviewDto buyer;
  private CategoryDto category;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  private LocalDateTime soldAt;
}