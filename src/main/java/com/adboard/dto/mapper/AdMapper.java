package com.adboard.dto.mapper;

import com.adboard.dto.request.AdCreateRequestDto;
import com.adboard.dto.request.AdUpdateRequestDto;
import com.adboard.dto.response.AdResponseDto;
import com.adboard.entity.Ad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {UserMapper.class, CategoryMapper.class})
public interface AdMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "seller", ignore = true)
  @Mapping(target = "category", ignore = true)
  @Mapping(target = "status", expression = "java(com.adboard.entity.enums.AdStatus.DRAFT)")
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "promoteExpiresAt", ignore = true)
  @Mapping(target = "soldAt", ignore = true)
  @Mapping(target = "buyer", ignore = true)
  @Mapping(target = "version", ignore = true)
  Ad toEntity(AdCreateRequestDto dto);

  void updateEntityFromDto(AdUpdateRequestDto dto, @MappingTarget Ad ad);

  AdResponseDto toResponseDto(Ad ad);
}