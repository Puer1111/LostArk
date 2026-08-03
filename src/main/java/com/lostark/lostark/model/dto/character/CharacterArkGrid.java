package com.lostark.lostark.model.dto.character;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lostark.lostark.model.dto.character.tooltip.Tooltip;
import com.lostark.lostark.model.dto.character.tooltip.TooltipDeserializer;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper; // Added ObjectMapper instance
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterArkGrid {

        @JsonProperty("Slots")
        private List<Slot> slots;
        @JsonProperty("Effects")
        private List<Effect> effects;

        private static final ObjectMapper objectMapper = new ObjectMapper(); // Added ObjectMapper instance

        @com.fasterxml.jackson.annotation.JsonIgnore
        public List<String> getAllCombinedGemEffects() {
            if (slots == null || slots.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            java.util.Map<String, Integer> effectLevels = new java.util.LinkedHashMap<>();
            
            for (Slot slot : slots) {
                if (slot.getGems() == null) continue;
                for (Gem gem : slot.getGems()) {
                    if (gem.getTooltip() == null) continue;
                    java.util.List<String> rawEffects = new java.util.ArrayList<>();
                    rawEffects.addAll(gem.getTooltip().getSimplifiedArkGridGemList("Element_005"));
                    rawEffects.addAll(gem.getTooltip().getSimplifiedArkGridGemList("Element_006"));
                    
                    for (String raw : rawEffects) {
                        java.util.regex.Pattern pEffect = java.util.regex.Pattern.compile("\\[?([^\\]\\s]+(?:\\s+[^\\]\\s]+)*)\\]?\\s*Lv\\.?\\s*(\\d+)");
                        java.util.regex.Matcher mEffect = pEffect.matcher(raw);
                        if (mEffect.find()) {
                            String name = mEffect.group(1).trim();
                            int lvl = Integer.parseInt(mEffect.group(2));
                            effectLevels.put(name, effectLevels.getOrDefault(name, 0) + lvl);
                        }
                    }
                }
            }
            
            java.util.List<String> combined = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, Integer> entry : effectLevels.entrySet()) {
                combined.add("[" + entry.getKey() + "] Lv." + entry.getValue());
            }
            return combined;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public int getAllCombinedGemLevelSum() {
            List<String> combined = getAllCombinedGemEffects();
            if (combined == null || combined.isEmpty()) {
                return 0;
            }
            int sum = 0;
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Lv\\.(\\d+)");
            for (String effect : combined) {
                java.util.regex.Matcher matcher = pattern.matcher(effect);
                if (matcher.find()) {
                    sum += Integer.parseInt(matcher.group(1));
                }
            }
            return sum;
        }




    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Slot {
        @JsonProperty("Index")
        private int index;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Point")
        private int point;
        @JsonProperty("Tooltip")
        @JsonDeserialize(using = TooltipDeserializer.class)
        private Tooltip tooltip; // Storing as String due to complex nested JSON structure
        @JsonProperty("Grade")
        private String grade;
        @JsonProperty("Gems")
        private List<Gem> gems;

        @com.fasterxml.jackson.annotation.JsonIgnore
        public List<String> getCombinedGemEffects() {
            if (gems == null || gems.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            java.util.Map<String, Integer> effectLevels = new java.util.LinkedHashMap<>();
            
            for (Gem gem : gems) {
                if (gem.getTooltip() == null) continue;
                java.util.List<String> rawEffects = new java.util.ArrayList<>();
                rawEffects.addAll(gem.getTooltip().getSimplifiedArkGridGemList("Element_005"));
                rawEffects.addAll(gem.getTooltip().getSimplifiedArkGridGemList("Element_006"));
                
                for (String raw : rawEffects) {
                    java.util.regex.Pattern pEffect = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]\\s*Lv\\.(\\d+)");
                    java.util.regex.Matcher mEffect = pEffect.matcher(raw);
                    if (mEffect.find()) {
                        String name = mEffect.group(1);
                        int lvl = Integer.parseInt(mEffect.group(2));
                        effectLevels.put(name, effectLevels.getOrDefault(name, 0) + lvl);
                    }
                }
            }
            
            java.util.List<String> combined = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, Integer> entry : effectLevels.entrySet()) {
                combined.add("[" + entry.getKey() + "] Lv." + entry.getValue());
            }
            return combined;
        }
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Gem {
        @JsonProperty("Index")
        private int index;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("IsActive")
        private boolean isActive;
        @JsonProperty("Grade")
        private String grade;
        @JsonProperty("Tooltip")
        @JsonDeserialize(using = TooltipDeserializer.class)
        private Tooltip tooltip; // Storing as String due to complex nested JSON structure

        public String getEffectsJson() {
            if (tooltip == null) {
                return "{}";
            }
            try {
                return objectMapper.writeValueAsString(tooltip.extractGemEffects());
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                System.err.println("Error serializing gem effects to JSON: " + e.getMessage()); // Log error
                return "{}";
            }
        }
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Effect {

        @JsonProperty("Name")
        private String name;
        @JsonProperty("Level")
        private int level;
        @JsonProperty("Tooltip")
        @JsonDeserialize(using = TooltipDeserializer.class)
        private Tooltip tooltip; // Contains HTML-like tags
    }
}
