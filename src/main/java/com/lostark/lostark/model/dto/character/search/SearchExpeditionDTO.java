package com.lostark.lostark.model.dto.character.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SearchExpeditionDTO {
    @JsonAlias("ServerName")
    private String serverName;
    @JsonAlias("CharacterName")
    private String characterName;
    @JsonAlias("CharacterClassName")
    private String characterClassName;
    @JsonAlias("CharacterLevel")
    private String characterLevel;
    @JsonAlias("ItemAvgLevel")
    private String itemAvgLevel;

    private String characterImage;
    private String combatPower;
}
