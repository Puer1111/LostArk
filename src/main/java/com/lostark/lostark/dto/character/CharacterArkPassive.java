package com.lostark.lostark.dto.character;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;

@Data
public class CharacterArkPassive {

    @JsonProperty("Title")
    private String title;
    @JsonProperty("IsArkPassive")
    private boolean isArkPassive;
    @JsonProperty("Points")
    private List<Point> points;
    @JsonProperty("Effects")
    private List<Effect> effects;

    @Data
    public static class Point {
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Value")
        private int value;
        @JsonProperty("Tooltip")
        private String tooltip;
        @JsonProperty("Description")
        private String description;
    }

    @Data
    public static class Effect {
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Description")
        private String description;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("ToolTip") // Note: "ToolTip" with capital 'T' in JSON
        private String toolTip;
    }
}
