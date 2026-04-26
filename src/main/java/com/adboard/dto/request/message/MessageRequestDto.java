package com.adboard.dto.request.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageRequestDto {

  @NotBlank(message = "Message content is required")
  @Size(max = 4000)
  private String content;

  private Long receiverId;
}