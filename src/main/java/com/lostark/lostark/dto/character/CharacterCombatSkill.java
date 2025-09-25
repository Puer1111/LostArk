package com.lostark.lostark.dto.character;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;
@Data
public class CharacterCombatSkill {

        @JsonProperty("Name")
        private String name;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("Level")
        private int level;
        @JsonProperty("Type")
        private String type;
        @JsonProperty("SkillType")
        private int skillType;
        @JsonProperty("Tripods")
        private List<Tripod> tripods;
        @JsonProperty("Rune")
        private Rune rune; // Can be null
        @JsonProperty("Tooltip")
        private String tooltip; // Storing as String due to complex nested JSON structure

    @Data
    public static class Tripod {
        @JsonProperty("Tier")
        private int tier;
        @JsonProperty("Slot")
        private int slot;
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("IsSelected")
        private boolean isSelected;
        @JsonProperty("Tooltip")
        private String tooltip; // Contains HTML-like tags
    }
    @Data
    public static class Rune {
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("Grade")
        private String grade;
        @JsonProperty("Tooltip")
        private String tooltip; // Storing as String due to complex nested JSON structure
    }
}
