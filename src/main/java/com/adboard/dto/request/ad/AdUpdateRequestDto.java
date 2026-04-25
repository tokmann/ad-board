package com.adboard.dto.request.ad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdUpdateRequestDto {

  @NotBlank(message = "Title is required")
  @Size(max = 100)
  private String title;

  @Size(max = 5000)
  private String description;

  @NotNull(message = "Price is required")
  @Positive(message = "Price must be greater than 0")
  private BigDecimal price;

  private String imageUrl;
}