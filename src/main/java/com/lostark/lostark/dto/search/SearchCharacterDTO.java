package com.lostark.lostark.dto.search;

import com.lostark.lostark.dto.character.*;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List; // Import List

@Data
@ToString
public class SearchCharacterDTO {
    @JsonProperty("ArmoryProfile")
    private CharacterProfiles characterProfiles;
    @JsonProperty("ArmoryEquipment")
    private List<CharacterEquipment> armoryEquipment;
    @JsonProperty("ArmoryAvatars")
    private List<CharacterAvatar.AvatarItem> armoryAvatars;
    @JsonProperty("ArmorySkills")
    private List<CharacterCombatSkill> armorySkills;
    @JsonProperty("ArmoryEngraving")
    private CharacterEngravings characterEngravings;
    @JsonProperty("ArmoryCard")
    private CharacterCards characterCards;
    @JsonProperty("ArmoryGem")
    private CharacterGems  characterGems;
    @JsonProperty("ColosseumInfo")
    private CharacterColosseum characterColosseum;
    @JsonProperty("Collectibles")
    private List<Charactercollectibles> collectibles;
    @JsonProperty("ArkPassive")
    private CharacterArkPassive characterArkPassive;
    @JsonProperty("ArkGrid")
    private CharacterArkGrid characterArkGrid;
}
