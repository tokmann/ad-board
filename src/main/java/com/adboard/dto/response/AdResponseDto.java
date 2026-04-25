package com.adboard.dto.response;

import com.adboard.entity.enums.AdStatus;
import lombok.Data;
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
  private LocalDateTime createdAt;
  private UserPreviewDto seller;
  private CategoryDto category;
}