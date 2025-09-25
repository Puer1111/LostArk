package com.lostark.lostark.dto.character;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;

@Data
public class CharacterEngravings {

    @JsonProperty("Engravings")
    private String engravings; // Can be null
    @JsonProperty("Effects")
    private String effects; // Can be null
    @JsonProperty("ArkPassiveEffects")
    private List<ArkPassiveEffect> arkPassiveEffects;

    @Data
    public static class ArkPassiveEffect {
        @JsonProperty("AbilityStoneLevel")
        private Integer abilityStoneLevel; // Can be null
        @JsonProperty("Grade")
        private String grade;
        @JsonProperty("Level")
        private int level;
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Description")
        private String description;
    }
}
