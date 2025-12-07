package com.devon.techblog.domain.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.devon.techblog.application.member.dto.SocialLinks;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SocialLinksConverter implements AttributeConverter<SocialLinks, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(SocialLinks attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to convert social links to json", e);
        }
    }

    @Override
    public SocialLinks convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return SocialLinks.empty();
        }
        try {
            return objectMapper.readValue(dbData, SocialLinks.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to convert json to social links", e);
        }
    }
}
