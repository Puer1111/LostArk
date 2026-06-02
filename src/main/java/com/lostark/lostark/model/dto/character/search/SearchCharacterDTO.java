package com.lostark.lostark.model.dto.character.search;

import com.lostark.lostark.model.dto.character.*;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List; // Import List

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchCharacterDTO {
    @JsonProperty("ArmoryProfile")
    private CharacterProfiles characterProfiles;
    @JsonProperty("ArmoryEquipment")
    private List<CharacterEquipment> characterEquipment;
    @JsonProperty("ArmoryAvatars")
    private List<CharacterAvatar> characterAvatars;
    @JsonProperty("ArmorySkills")
    private List<CharacterCombatSkill> characterCombatSkill;
    @JsonProperty("ArmoryEngraving")
    private CharacterEngravings characterEngravings;
    @JsonProperty("ArmoryCard")
    private CharacterCards characterCards;
    @JsonProperty("ArmoryGem")
    private CharacterGems  characterGems;
    @JsonProperty("ColosseumInfo")
    private CharacterColosseum characterColosseum;
    @JsonProperty("Collectibles")
    private List<CharacterCollectibles> characterCollectibles;
    @JsonProperty("ArkPassive")
    private CharacterArkPassive characterArkPassive;
    @JsonProperty("ArkGrid")
    private CharacterArkGrid characterArkGrid;

    private List<SearchExpeditionDTO> expeditions;
}
