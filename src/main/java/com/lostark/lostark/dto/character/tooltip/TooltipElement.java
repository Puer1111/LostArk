package com.lostark.lostark.dto.character.tooltip;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // json 에 isInner , isSet 이런거 무시
public class TooltipElement {
    @JsonProperty("type")
    private String type;

    @JsonProperty("value")
    private Object value;
}
