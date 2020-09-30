package org.cloudfoundry.multiapps.controller.persistence.dto;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.commons.lang3.StringUtils;

@Converter
public class TextAttributeConverter implements AttributeConverter<String, String> {

    private static final int MAX_STRING_LENGTH = 4000;

    @Override
    public String convertToDatabaseColumn(String text) {
        if (text != null) {
            return StringUtils.abbreviate(text, MAX_STRING_LENGTH);
        }
        return "";
    }

    @Override
    public String convertToEntityAttribute(String text) {
        return text;
    }

}
