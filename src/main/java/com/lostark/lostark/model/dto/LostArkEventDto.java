package com.lostark.lostark.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LostArkEventDto {
    @JsonProperty("Title")
    private String title;

    @JsonProperty("Thumbnail")
    private String thumbnail;

    @JsonProperty("Link")
    private String link;

    @JsonProperty("StartDate")
    private LocalDateTime startDate;

    @JsonProperty("EndDate")
    private LocalDateTime endDate;

    @JsonProperty("RewardDate")
    private LocalDateTime rewardDate;
}
