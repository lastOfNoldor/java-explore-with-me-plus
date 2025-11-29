package ru.practicum.main_service.category.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.main_service.category.dto.CategoryDto;
import ru.practicum.main_service.category.dto.NewCategoryDto;
import ru.practicum.main_service.category.model.Category;

@Component
public class CategoryMapper {

    public Category toCategory(NewCategoryDto newCategoryDto) {
        return Category.builder().name(newCategoryDto.getName()).build();
    }

    public Category toCategory(CategoryDto categoryDto) {
        return Category.builder().id(categoryDto.getId()).name(categoryDto.getName()).build();
    }

    public CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }

    public CategoryDto toCategoryDto(NewCategoryDto newCategoryDto) {
        return new CategoryDto(null, newCategoryDto.getName());
    }

}
