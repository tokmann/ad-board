package com.adboard.dto.mapper;

import com.adboard.dto.request.ReviewRequestDto;
import com.adboard.dto.response.ReviewResponseDto;
import com.adboard.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = UserMapper.class)
public interface ReviewMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "reviewer", ignore = true)
  @Mapping(target = "seller", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  Review toEntity(ReviewRequestDto dto);

  ReviewResponseDto toResponseDto(Review review);
}