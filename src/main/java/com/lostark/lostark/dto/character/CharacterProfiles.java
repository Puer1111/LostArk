package com.lostark.lostark.dto.character;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;
@Data
public class CharacterProfiles {
    @JsonProperty("CharacterImage")
    private String characterImage;
    @JsonProperty("ExpeditionLevel")
    private int expeditionLevel;
    @JsonProperty("PvpGradeName")
    private String pvpGradeName;
    @JsonProperty("TownLevel")
    private int townLevel;
    @JsonProperty("TownName")
    private String townName;
    @JsonProperty("Title")
    private String title;
    @JsonProperty("GuildMemberGrade")
    private String guildMemberGrade;
    @JsonProperty("GuildName")
    private String guildName;
    @JsonProperty("UsingSkillPoint")
    private int usingSkillPoint;
    @JsonProperty("TotalSkillPoint")
    private int totalSkillPoint;
    @JsonProperty("Stats")
    private List<Stat> stats;
    @JsonProperty("Tendencies")
    private List<Tendency> tendencies;
    @JsonProperty("CombatPower")
    private String combatPower;
    @JsonProperty("Decorations")
    private Decorations decorations; // Corrected: should be Decorations object, not List<Decorations>
    @JsonProperty("HonorPoint")
    private int honorPoint;
    @JsonProperty("ServerName")
    private String serverName;
    @JsonProperty("CharacterName")
    private String characterName;
    @JsonProperty("CharacterLevel")
    private int characterLevel;
    @JsonProperty("CharacterClassName")
    private String characterClassName;
    @JsonProperty("ItemAvgLevel")
    private String itemAvgLevel;

    // Inner class for Stats
    @Data
    public static class Stat {
        @JsonProperty("Type")
        private String type;
        @JsonProperty("Value")
        private String value;
        @JsonProperty("Tooltip")
        private List<String> tooltip;
    }

    // Inner class for Tendencies
    @Data
    public static class Tendency {
        @JsonProperty("Type")
        private String type;
        @JsonProperty("Point")
        private int point;
        @JsonProperty("MaxPoint")
        private int maxPoint;
    }
    @Data
    public static class Decorations {
        @JsonProperty("Symbol")
        private String symbol;
        @JsonProperty("Emblems")
        private String emblems;
    }
}