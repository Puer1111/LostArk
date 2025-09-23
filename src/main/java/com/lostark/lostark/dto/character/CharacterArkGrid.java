package com.lostark.lostark.dto.character;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;
@Data
public class CharacterArkGrid {
//    @JsonProperty("ArkGrid")
//    private ArkGrid arkGrid;
//    @Data
//    public static class ArkGrid {
        @JsonProperty("Slots")
        private List<Slot> slots;
        @JsonProperty("Effects")
        private List<Effect> effects;
//    }
    @Data
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
        private String tooltip; // Storing as String due to complex nested JSON structure
        @JsonProperty("Grade")
        private String grade;
        @JsonProperty("Gems")
        private List<Gem> gems;
    }
    @Data
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
        private String tooltip; // Storing as String due to complex nested JSON structure
    }
    @Data
    public static class Effect {
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Level")
        private int level;
        @JsonProperty("Tooltip")
        private String tooltip; // Contains HTML-like tags
    }
}
