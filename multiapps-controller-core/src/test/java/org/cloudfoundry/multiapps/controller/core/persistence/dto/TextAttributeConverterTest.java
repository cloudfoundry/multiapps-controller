package org.cloudfoundry.multiapps.controller.core.persistence.dto;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TextAttributeConverterTest {

    private final TextAttributeConverter textAttributeConverter = new TextAttributeConverter();

    @Test
    void testConvertToEntityAttribute() {
        Assertions.assertEquals("test", textAttributeConverter.convertToEntityAttribute("test"));
    }

    @Test
    void testConvertToDatabaseColumnWhenInputIsNotNull() {
        Assertions.assertEquals("test", textAttributeConverter.convertToDatabaseColumn("test"));
    }

    @Test
    void testConvertToDatabaseColumnWhenInputIsNull() {
        Assertions.assertEquals("", textAttributeConverter.convertToDatabaseColumn(null));
    }

    @Test
    void testConvertToDatabaseColumnWhenInputIsLongerThan4000chars() {
        String expectedString = getLongStringInput().substring(0, 3997)
                                                    .concat("...");

        Assertions.assertEquals(expectedString, textAttributeConverter.convertToDatabaseColumn(getLongStringInput()));
    }

    private String getLongStringInput() {
        int stringLength = 5000;
        return StringUtils.repeat("a", stringLength);
    }
}
