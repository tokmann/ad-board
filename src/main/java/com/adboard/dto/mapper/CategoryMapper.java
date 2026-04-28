package com.adboard.dto.mapper;

import com.adboard.dto.response.category.CategoryDto;
import com.adboard.entity.reference.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CategoryMapper {

  CategoryDto toDto(Category category);
}