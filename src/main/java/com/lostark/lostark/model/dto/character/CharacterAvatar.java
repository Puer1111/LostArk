package com.lostark.lostark.model.dto.character;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lostark.lostark.model.dto.character.tooltip.Tooltip;
import com.lostark.lostark.model.dto.character.tooltip.TooltipDeserializer;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import


@Data
public class CharacterAvatar {

    @JsonProperty("Type")
    private String type;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Icon")
    private String icon;
    @JsonProperty("Grade")
    private String grade;
    @JsonProperty("IsSet")
    private boolean isSet;
    @JsonProperty("IsInner")
    private boolean isInner;
    @JsonProperty("Tooltip")
    @JsonDeserialize(using = TooltipDeserializer.class)
    private Tooltip tooltip; // Storing as String due to complex nested JSON structure

    @lombok.Data
    public static class DyeInfo {
        private String part; // 부위 (예: "부위1", "부위2" 등)
        private String baseColor; // 기본 색상 (예: "#FFEEF0")
        private String glossValue; // 광택 (예: "기본값", "0%")
        private String patternColor; // 패턴 색상
        private String iconPath; // 패턴 아이콘 경로
    }

    /**
     * Tooltip JSON 내용 중 ItemTintGroup 구조에서 염색 정보(부위, baseColor, glossValue, patternColor, iconPath)를 파싱하여 반환합니다.
     */
    public java.util.List<DyeInfo> getDyeInfoList() {
        java.util.List<DyeInfo> dyeList = new java.util.ArrayList<>();
        if (tooltip == null || tooltip.getElements() == null) {
            return dyeList;
        }

        for (java.util.Map.Entry<String, com.lostark.lostark.model.dto.character.tooltip.TooltipElement> entry : tooltip.getElements().entrySet()) {
            com.lostark.lostark.model.dto.character.tooltip.TooltipElement element = entry.getValue();
            if (element == null) {
                continue;
            }

            // 아바타 염색 정보는 Type이 "ItemTintGroup"인 요소에 저장되어 있습니다.
            if ("ItemTintGroup".equals(element.getType())) {
                Object valObj = element.getValue();
                if (valObj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> valueMap = (java.util.Map<String, Object>) valObj;

                    // value 맵 안의 itemData를 조회
                    Object itemDataObj = valueMap.get("itemData");
                    if (itemDataObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> itemDataMap = (java.util.Map<String, Object>) itemDataObj;

                        // itemData 내부의 각 Element_000, Element_001, Element_002 등 조회
                        // 키 순서 보장을 위해 정렬
                        java.util.List<String> sortedKeys = new java.util.ArrayList<>(itemDataMap.keySet());
                        java.util.Collections.sort(sortedKeys);

                        for (String innerKey : sortedKeys) {
                            Object innerValObj = itemDataMap.get(innerKey);
                            if (innerValObj instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> partMap = (java.util.Map<String, Object>) innerValObj;

                                DyeInfo dyeInfo = new DyeInfo();

                                // 1. 부위 명칭 (title)
                                dyeInfo.setPart(String.valueOf(partMap.getOrDefault("title", "")));

                                // 2. baseColor
                                dyeInfo.setBaseColor(String.valueOf(partMap.getOrDefault("baseColor", "")));

                                // 3. glossValue
                                dyeInfo.setGlossValue(String.valueOf(partMap.getOrDefault("glossValue", "")));

                                // 4. patternColor
                                dyeInfo.setPatternColor(String.valueOf(partMap.getOrDefault("patternColor", "")));

                                // 5. patternIcon -> iconPath 추출
                                Object iconObj = partMap.get("patternIcon");
                                if (iconObj instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, Object> iconMap = (java.util.Map<String, Object>) iconObj;
                                    dyeInfo.setIconPath(String.valueOf(iconMap.getOrDefault("iconPath", "")));
                                }

                                dyeList.add(dyeInfo);
                            }
                        }
                    }
                }
            }
        }
        return dyeList;
    }
}
