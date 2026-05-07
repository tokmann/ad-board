package com.adboard.dto.request.ad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdCreateRequestDto {

  @NotBlank(message = "Title is required")
  @Size(max = 100, message = "Title is too long")
  private String title;

  @Size(max = 5000, message = "Description is too long")
  private String description;

  @NotNull(message = "Price is required")
  @Positive(message = "Price must be greater than 0")
  private BigDecimal price;

  @NotNull(message = "Category is required")
  private Long categoryId;

  private String imageUrl;
}