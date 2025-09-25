package com.lostark.lostark.dto.character.tooltip;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class Tooltip {
    private Map<String, TooltipElement> elements = new HashMap<>();

    @JsonAnySetter
    public void addElement(String name, TooltipElement value) {
        elements.put(name, value);
    }
}
