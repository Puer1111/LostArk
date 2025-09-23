package com.lostark.lostark.dto.character;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CharacterColosseum {

    @JsonProperty("Rank")
    private int rank;

    @JsonProperty("PreRank")
    private int preRank;

    @JsonProperty("Exp")
    private int exp;

    @JsonProperty("Colosseums")
    private List<Colosseum> colosseums;

    @Data
    public static class Colosseum {
        @JsonProperty("SeasonName")
        private String seasonName;

        @JsonProperty("Competitive")
        private GameModeStats competitive;

        @JsonProperty("TeamDeathmatch")
        private GameModeStats teamDeathmatch;

        @JsonProperty("TeamElimination")
        private GameModeStats teamElimination;

        @JsonProperty("CoOpBattle")
        private GameModeStats coOpBattle;

        @JsonProperty("OneDeathmatch")
        private GameModeStats oneDeathmatch;

        @JsonProperty("OneDeathmatchRank")
        private GameModeStats oneDeathmatchRank;
    }

    @Data
    public static class GameModeStats {
        @JsonProperty("PlayCount")
        private int playCount;

        @JsonProperty("VictoryCount")
        private int victoryCount;

        @JsonProperty("LoseCount")
        private int loseCount;

        @JsonProperty("TieCount")
        private int tieCount;

        @JsonProperty("KillCount")
        private int killCount;

        @JsonProperty("AceCount")
        private int aceCount;

        @JsonProperty("DeathCount")
        private int deathCount;

        @JsonProperty("AssistCount")
        private Integer assistCount; // Not present in all modes, so use Integer to allow null
    }
}