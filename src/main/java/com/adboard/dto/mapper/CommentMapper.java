package com.adboard.dto.mapper;

import com.adboard.dto.request.CommentRequestDto;
import com.adboard.dto.response.CommentResponseDto;
import com.adboard.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = UserMapper.class)
public interface CommentMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "ad", ignore = true)
  @Mapping(target = "author", ignore = true)
  @Mapping(target = "parentComment", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  Comment toEntity(CommentRequestDto dto);

  void updateEntityFromDto(CommentRequestDto dto, @MappingTarget Comment comment);

  CommentResponseDto toResponseDto(Comment comment);
}