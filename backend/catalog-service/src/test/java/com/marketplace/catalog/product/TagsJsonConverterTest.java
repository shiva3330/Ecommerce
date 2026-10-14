package com.marketplace.catalog.product;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TagsJsonConverterTest {

    private final TagsJsonConverter converter = new TagsJsonConverter();

    @Test
    void roundTripsTagsWithoutDelimiterEscapingConcerns() {
        List<String> tags = List.of("small, batch", "quote \"", "[limited]");

        String databaseValue = converter.convertToDatabaseColumn(tags);

        assertThat(converter.convertToEntityAttribute(databaseValue)).containsExactlyElementsOf(tags);
    }

    @Test
    void treatsMissingTagsAsAnEmptyList() {
        assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("[]");
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }
}
