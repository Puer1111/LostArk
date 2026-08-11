package com.lostark.lostark.model.dto.character;

import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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
    private CharacterGems characterGems;
    @JsonProperty("ColosseumInfo")
    private CharacterColosseum characterColosseum;
    @JsonProperty("Collectibles")
    private List<CharacterCollectibles> characterCollectibles;
    @JsonProperty("ArkPassive")
    private CharacterArkPassive characterArkPassive;
    @JsonProperty("ArkGrid")
    private CharacterArkGrid characterArkGrid;

    private List<SearchExpeditionDTO> expeditions;

    public void applyDealerStatusToGrid() {
        if (characterArkGrid == null) {
            return;
        }
        
        boolean isDealer = true;
        if (characterArkPassive != null && characterArkPassive.getTitle() != null) {
            String title = characterArkPassive.getTitle();
            if (title.contains("만개") || 
                title.contains("절실한 구원") || 
                title.contains("해방자") || 
                title.contains("축복의 오라")) {
                isDealer = false;
            }
        }
        
        characterArkGrid.setDealer(isDealer);
    }
}
