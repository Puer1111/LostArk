package com.lostark.lostark.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LostArkCalendar {
    @JsonProperty("CategoryName")
    private String categoryName;
    
    @JsonProperty("ContentsName")
    private String contentsName;
    
    @JsonProperty("ContentsIcon")
    private String contentsIcon;
    
    @JsonProperty("MinItemLevel")
    private Integer minItemLevel;
    
    @JsonProperty("StartTimes")
    private List<LocalDateTime> startTimes;
    
    @JsonProperty("Location")
    private String location;
    
    @JsonProperty("RewardItems")
    private List<RewardItemLevel> rewardItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardItemLevel {
        @JsonProperty("ItemLevel")
        private Integer itemLevel;
        
        @JsonProperty("Items")
        private List<RewardItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardItem {
        @JsonProperty("Name")
        private String name;
        
        @JsonProperty("Icon")
        private String icon;
        
        @JsonProperty("Grade")
        private String grade;

        @JsonProperty("StartTimes")
        private List<LocalDateTime> startTimes;

        // null 방지를 위한 Getter 커스텀
        public List<LocalDateTime> getStartTimes() {
            return startTimes != null ? startTimes : new ArrayList<>();
        }
    }
}
