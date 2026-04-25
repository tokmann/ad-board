package com.adboard.dto.mapper;

import com.adboard.dto.response.category.CategoryDto;
import com.adboard.entity.reference.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CategoryMapper {

  CategoryDto toDto(Category category);
}