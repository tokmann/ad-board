package com.adboard.dto.mapper;

import com.adboard.dto.request.RegisterRequestDto;
import com.adboard.dto.response.UserPreviewDto;
import com.adboard.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "passwordHash", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "roles", ignore = true)
  User toEntity(RegisterRequestDto dto);

  UserPreviewDto toPreviewDto(User user);
}