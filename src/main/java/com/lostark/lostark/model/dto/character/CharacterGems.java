package com.lostark.lostark.model.dto.character;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lostark.lostark.model.dto.character.tooltip.Tooltip;
import com.lostark.lostark.model.dto.character.tooltip.TooltipDeserializer;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;

@Data
public class CharacterGems {

    @JsonProperty("Gems")
    private List<Gem> gems;
    @JsonProperty("Effects")
    private GemEffects effects;

    @Data
    public static class Gem {
        @JsonProperty("Slot")
        private int slot;
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("Level")
        private int level;
        @JsonProperty("Grade")
        private String grade;
        @JsonProperty("Tooltip")
        @JsonDeserialize(using = TooltipDeserializer.class)
        private Tooltip tooltip; // Storing as String due to complex nested JSON structure
    }

    @Data
    public static class GemEffects {
        @JsonProperty("Description")
        private String description;
        @JsonProperty("Skills")
        private List<Skill> skills;
    }

    @Data
    public static class Skill {
        @JsonProperty("GemSlot")
        private int gemSlot;
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Description")
        private List<String> description;
        @JsonProperty("Option")
        private String option;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("Tooltip")
        @JsonDeserialize(using = TooltipDeserializer.class)
        private Tooltip tooltip; // Storing as String due to complex nested JSON structure
    }
}
