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

        private boolean isDealer = true;

        void setDealer(boolean dealer) {
            this.isDealer = dealer;
            if (this.slots != null) {
                for (Slot slot : this.slots) {
                    slot.setDealer(dealer);
                }
            }
        }

        private static final ObjectMapper objectMapper = new ObjectMapper(); // Added ObjectMapper instance

        @com.fasterxml.jackson.annotation.JsonIgnore
        public List<String> getAllCombinedGemEffects() {
            return getAllCombinedGemEffects(this.isDealer); // Default to current isDealer sorting
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public List<String> getAllCombinedGemEffects(boolean isDealer) {
            if (slots == null || slots.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            List<Gem> allGems = new java.util.ArrayList<>();
            for (Slot slot : slots) {
                if (slot.getGems() != null) {
                    allGems.addAll(slot.getGems());
                }
            }
            return sortEffects(combineGemEffects(allGems), isDealer);
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

        private static List<String> combineGemEffects(List<Gem> gems) {
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
                    java.util.regex.Pattern pEffect = java.util.regex.Pattern.compile("\\[?([^\\]\\s]+(?:\\s+[^\\]\\s]+)*)\\]?\\s*[lL][vV]\\.?\\s*(\\d+)");
                    java.util.regex.Matcher mEffect = pEffect.matcher(raw);
                    if (mEffect.find()) {
                        String name = mEffect.group(1).trim();
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

        private static List<String> sortEffects(List<String> combinedEffects, boolean isDealer) {
            List<String> dealerOrder = java.util.Arrays.asList("공격력", "보스 피해", "추가 피해", "낙인력", "아군 공격 강화", "아군 피해 강화");
            List<String> supportOrder = java.util.Arrays.asList("낙인력", "아군 공격 강화", "아군 피해 강화", "공격력", "보스 피해", "추가 피해");
            List<String> order = isDealer ? dealerOrder : supportOrder;

            java.util.List<String> sorted = new java.util.ArrayList<>(combinedEffects);
            sorted.sort((e1, e2) -> {
                String name1 = extractEffectName(e1);
                String name2 = extractEffectName(e2);

                int idx1 = -1;
                int idx2 = -1;
                
                for (int i = 0; i < order.size(); i++) {
                    if (name1.contains(order.get(i))) {
                        idx1 = i;
                        break;
                    }
                }
                for (int i = 0; i < order.size(); i++) {
                    if (name2.contains(order.get(i))) {
                        idx2 = i;
                        break;
                    }
                }

                if (idx1 != -1 && idx2 != -1) {
                    return Integer.compare(idx1, idx2);
                } else if (idx1 != -1) {
                    return -1;
                } else if (idx2 != -1) {
                    return 1;
                }
                return name1.compareTo(name2);
            });

            return sorted;
        }

        private static String extractEffectName(String effectStr) {
            if (effectStr.startsWith("[") && effectStr.contains("]")) {
                return effectStr.substring(1, effectStr.indexOf("]")).trim();
            }
            return effectStr;
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

        private boolean isDealer = true;

        void setDealer(boolean dealer) {
            this.isDealer = dealer;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public List<String> getCombinedGemEffects() {
            return getCombinedGemEffects(this.isDealer); // Default to current isDealer sorting
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public List<String> getCombinedGemEffects(boolean isDealer) {
            return sortEffects(combineGemEffects(this.gems), isDealer);
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
