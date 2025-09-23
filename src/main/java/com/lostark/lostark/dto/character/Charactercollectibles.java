package com.lostark.lostark.dto.character;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;
@Data
public class Charactercollectibles {
//    @JsonProperty("Collectibles")
//    private List<Collectible> collectibles;
//    @Data
//    public static class Collectible {
        @JsonProperty("Type")
        private String type;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("Point")
        private int point;
        @JsonProperty("MaxPoint")
        private int maxPoint;
        @JsonProperty("CollectiblePoints")
        private List<CollectiblePoint> collectiblePoints;
//    }
    @Data
    public static class CollectiblePoint {
        @JsonProperty("PointName")
        private String pointName;
        @JsonProperty("Point")
        private int point;
        @JsonProperty("MaxPoint")
        private int maxPoint;
    }
}
