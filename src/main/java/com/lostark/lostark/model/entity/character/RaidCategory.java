package com.lostark.lostark.model.entity.character;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RaidCategory {
    LEGION_COMMANDER("군단장"),
    KAZEROS("카제로스"),
    SHADOW("그림자"),
    EPIC("에픽"),
    ABYSS("어비스 던전");

    private final String displayName;

    RaidCategory(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static RaidCategory fromDisplayName(String value) {
        for (RaidCategory category : values()) {
            if (category.displayName.equals(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 레이드 종류입니다: " + value);
    }
}
