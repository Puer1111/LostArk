package com.lostark.lostark.model.entity.character;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RaidCategoryConverter implements AttributeConverter<RaidCategory, String> {

    @Override
    public String convertToDatabaseColumn(RaidCategory category) {
        return category == null ? null : category.getDisplayName();
    }

    @Override
    public RaidCategory convertToEntityAttribute(String value) {
        return value == null ? null : RaidCategory.fromDisplayName(value);
    }
}
