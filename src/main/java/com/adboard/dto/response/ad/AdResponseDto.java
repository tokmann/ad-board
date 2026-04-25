package com.adboard.dto.response.ad;

import com.adboard.dto.response.category.CategoryDto;
import com.adboard.dto.response.UserPreviewDto;
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
  private LocalDateTime promoteExpiresAt;
  private LocalDateTime createdAt;
  private UserPreviewDto seller;
  private CategoryDto category;
}