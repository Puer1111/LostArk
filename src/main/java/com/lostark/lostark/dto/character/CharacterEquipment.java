package com.lostark.lostark.dto.character;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lostark.lostark.dto.character.tooltip.Tooltip;
import com.lostark.lostark.dto.character.tooltip.TooltipDeserializer;
import lombok.Data;

@Data
public class CharacterEquipment {
    @JsonProperty("Type")
    private String type;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Icon")
    private String icon;
    @JsonProperty("Grade")
    private String grade;

    @JsonProperty("Tooltip")
    @JsonDeserialize(using = TooltipDeserializer.class)
    private Tooltip tooltip;

    @JsonProperty("EquipOptions")
    private String equipOptions; // Can be null

    @JsonProperty("AccessoryOptions")
    private String accessoryOptions; // Can be null

}
