package com.lostark.lostark.model.entity.character;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RaidDifficulty {
    NORMAL("노말"),
    HARD("하드"),
    NIGHTMARE("나이트메어"),
    EXTREME("익스트림"),
    STAGE_1("1단계"),
    STAGE_2("2단계"),
    STAGE_3("3단계"),
    MATCHING("매칭");

    private final String displayName;

    RaidDifficulty(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static RaidDifficulty fromDisplayName(String value) {
        for (RaidDifficulty difficulty : values()) {
            if (difficulty.displayName.equals(value)) {
                return difficulty;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 레이드 난이도입니다: " + value);
    }
}
