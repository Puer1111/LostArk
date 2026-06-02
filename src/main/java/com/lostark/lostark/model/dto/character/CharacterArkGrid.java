package com.lostark.lostark.model.dto.character;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lostark.lostark.model.dto.character.tooltip.Tooltip;
import com.lostark.lostark.model.dto.character.tooltip.TooltipDeserializer;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper; // Added ObjectMapper instance
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterArkGrid {

        @JsonProperty("Slots")
        private List<Slot> slots;
        @JsonProperty("Effects")
        private List<Effect> effects;

        private static final ObjectMapper objectMapper = new ObjectMapper(); // Added ObjectMapper instance



    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Slot {
        @JsonProperty("Index")
        private int index;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Point")
        private int point;
        @JsonProperty("Tooltip")
        @JsonDeserialize(using = TooltipDeserializer.class)
        private Tooltip tooltip; // Storing as String due to complex nested JSON structure
        @JsonProperty("Grade")
        private String grade;
        @JsonProperty("Gems")
        private List<Gem> gems;
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Gem {
        @JsonProperty("Index")
        private int index;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("IsActive")
        private boolean isActive;
        @JsonProperty("Grade")
        private String grade;
        @JsonProperty("Tooltip")
        @JsonDeserialize(using = TooltipDeserializer.class)
        private Tooltip tooltip; // Storing as String due to complex nested JSON structure

        public String getEffectsJson() {
            if (tooltip == null) {
                return "{}";
            }
            try {
                return objectMapper.writeValueAsString(tooltip.extractGemEffects());
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                System.err.println("Error serializing gem effects to JSON: " + e.getMessage()); // Log error
                return "{}";
            }
        }
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Effect {

        @JsonProperty("Name")
        private String name;
        @JsonProperty("Level")
        private int level;
        @JsonProperty("Tooltip")
        @JsonDeserialize(using = TooltipDeserializer.class)
        private Tooltip tooltip; // Contains HTML-like tags
    }
}
