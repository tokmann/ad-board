package com.adboard.dto.mapper;

import com.adboard.dto.request.comment.CommentRequestDto;
import com.adboard.dto.response.comment.CommentResponseDto;
import com.adboard.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = UserMapper.class,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
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