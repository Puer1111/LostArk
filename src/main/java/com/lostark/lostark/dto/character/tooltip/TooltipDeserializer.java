package com.lostark.lostark.dto.character.tooltip;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class TooltipDeserializer extends JsonDeserializer<Tooltip> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Tooltip deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String json = p.getText();
        if (json == null || json.isEmpty()) {
            return null;
        }
        return objectMapper.readValue(json, Tooltip.class);
    }
}
