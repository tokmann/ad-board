package com.adboard.dto.mapper;

import com.adboard.dto.request.MessageRequestDto;
import com.adboard.dto.response.MessageResponseDto;
import com.adboard.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = UserMapper.class)
public interface MessageMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "sender", ignore = true)
  @Mapping(target = "receiver", ignore = true)
  @Mapping(target = "ad", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "isRead", constant = "false")
  Message toEntity(MessageRequestDto dto);

  MessageResponseDto toResponseDto(Message message);
}