package com.lostark.lostark.model.dto.character;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lostark.lostark.model.dto.character.tooltip.Tooltip;
import com.lostark.lostark.model.dto.character.tooltip.TooltipDeserializer;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;
@Data
public class CharacterCards {

        @JsonProperty("Cards")
        private List<Card> cards;
        @JsonProperty("Effects")
        private List<Effect> effects;

    @Data
    public static class Card {
        @JsonProperty("Slot")
        private int slot;
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("AwakeCount")
        private int awakeCount;
        @JsonProperty("AwakeTotal")
        private int awakeTotal;
        @JsonProperty("Grade")
        private String grade;
        @JsonProperty("Tooltip")
        @JsonDeserialize(using = TooltipDeserializer.class)
        private Tooltip tooltip; // Storing as String due to complex nested JSON structure
    }
    @Data
    public static class Effect {
        @JsonProperty("Index")
        private int index;
        @JsonProperty("CardSlots")
        private List<Integer> cardSlots;
        @JsonProperty("Items")
        private List<EffectItem> items;
    }
    @Data
    public static class EffectItem {
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Description")
        private String description;
    }
}
