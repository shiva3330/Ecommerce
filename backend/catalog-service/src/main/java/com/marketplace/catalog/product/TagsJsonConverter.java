package com.marketplace.catalog.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/** Stores the storefront's string tags as JSON in a portable text column. */
@Converter
public class TagsJsonConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TAG_LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Product tags could not be serialized", exception);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String databaseValue) {
        if (databaseValue == null || databaseValue.isBlank()) {
            return List.of();
        }

        try {
            return List.copyOf(OBJECT_MAPPER.readValue(databaseValue, TAG_LIST_TYPE));
        } catch (JsonProcessingException | RuntimeException exception) {
            throw new IllegalArgumentException("Product tags could not be deserialized", exception);
        }
    }
}
