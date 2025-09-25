package com.lostark.lostark.dto.character.tooltip;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TooltipElement {
    @JsonProperty("type")
    private String type;

    @JsonProperty("value")
    private Object value;
}
