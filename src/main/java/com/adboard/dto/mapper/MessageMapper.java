package com.adboard.dto.mapper;

import com.adboard.dto.request.message.MessageRequestDto;
import com.adboard.dto.response.message.MessageResponseDto;
import com.adboard.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = UserMapper.class,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MessageMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "conversation", ignore = true)
  @Mapping(target = "sender", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "read", constant = "false")
  Message toEntity(MessageRequestDto dto);

  MessageResponseDto toResponseDto(Message message);
}