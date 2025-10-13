package com.lostark.lostark.model.dto.character.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SearchExpeditionDTO {
    @JsonProperty("ServerName")
    private String serverName;
    @JsonProperty("CharacterName")
    private String characterName;
    @JsonProperty("CharacterClassName")
    private String characterClassName;
    @JsonProperty("CharacterLevel")
    private String characterLevel;
    @JsonProperty("ItemAvgLevel")
    private String itemAvgLevel;
}
