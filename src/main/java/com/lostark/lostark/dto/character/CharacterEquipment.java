package com.lostark.lostark.dto.character;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;
@Data
public class CharacterEquipment {
//    @JsonProperty("ArmoryEquipment")
//    private List<EquipmentItem> armoryEquipment;
//    @Data
//    public static class EquipmentItem {
        @JsonProperty("Type")
        private String type;
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("Grade")
        private String grade;
        @JsonProperty("Tooltip")
        private String tooltip; // Storing as String due to complex nested JSON structure
        @JsonProperty("EquipOptions")
        private String equipOptions; // Can be null
        @JsonProperty("AccessoryOptions")
        private String accessoryOptions; // Can be null
//    }
}
