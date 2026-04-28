package com.adboard.dto.mapper;

import com.adboard.dto.request.ad.AdCreateRequestDto;
import com.adboard.dto.request.ad.AdUpdateRequestDto;
import com.adboard.dto.response.ad.AdResponseDto;
import com.adboard.entity.Ad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {UserMapper.class, CategoryMapper.class},
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
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