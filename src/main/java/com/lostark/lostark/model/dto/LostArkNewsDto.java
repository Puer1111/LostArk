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
public class LostArkNewsDto {
    @JsonProperty("Type")
    private String type;

    @JsonProperty("Title")
    private String title;

    @JsonProperty("Date")
    private LocalDateTime date;

    @JsonProperty("Link")
    private String link;
}
