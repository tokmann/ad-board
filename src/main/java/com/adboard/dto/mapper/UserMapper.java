package com.adboard.dto.mapper;

import com.adboard.dto.request.user.ProfileUpdateRequestDto;
import com.adboard.dto.request.auth.RegisterRequestDto;
import com.adboard.dto.response.UserPreviewDto;
import com.adboard.dto.response.user.UserProfileDto;
import com.adboard.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "passwordHash", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "roles", ignore = true)
  User toEntity(RegisterRequestDto dto);

  UserPreviewDto toPreviewDto(User user);

  UserProfileDto toProfileDto(User user);

  void updateEntityFromDto(ProfileUpdateRequestDto dto, @MappingTarget User user);
}