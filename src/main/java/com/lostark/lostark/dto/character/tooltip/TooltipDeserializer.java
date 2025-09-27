package com.lostark.lostark.dto.character.tooltip;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class TooltipDeserializer extends JsonDeserializer<Tooltip> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Tooltip deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String rawStringValue = p.getText(); // 원시 문자열로 값 가져옴
        if (rawStringValue == null || rawStringValue.isEmpty()) {
            return null;
        }

        if (rawStringValue.startsWith("{") && rawStringValue.endsWith("}")) {
            // JSON 객체 형식인지 확인
            try {
                return objectMapper.readValue(rawStringValue, Tooltip.class);
            } catch (JsonProcessingException e) {

            }
        }

        Tooltip tooltip = new Tooltip();
        TooltipElement element = new TooltipElement();
        element.setType("RawHtml"); // Custom type for raw HTML
        element.setValue(rawStringValue);
        tooltip.addElement("Element_000", element);
        return tooltip;
    }
}