package com.lostark.lostark.model.dto.character;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lostark.lostark.model.dto.character.tooltip.Tooltip;
import com.lostark.lostark.model.dto.character.tooltip.TooltipDeserializer;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;

@Data
public class CharacterArkPassive {

    @JsonProperty("Title")
    private String title;
    @JsonProperty("IsArkPassive")
    private boolean isArkPassive;
    @JsonProperty("Points")
    private List<Point> points;
    @JsonProperty("Effects")
    private List<Effect> effects;

    @Data
    public static class Point {
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Value")
        private int value;
        @JsonProperty("Tooltip")
        @JsonDeserialize(using = TooltipDeserializer.class)
        private Tooltip tooltip;
        @JsonProperty("Description")
        private String description;
    }

    @Data
    public static class Effect {
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Description")
        private String description;
        @JsonProperty("Icon")
        private String icon;
        @JsonProperty("ToolTip") // Note: "ToolTip" with capital 'T' in JSON
        @JsonDeserialize(using = TooltipDeserializer.class)
        private Tooltip toolTip;

        /**
         * Description에서 티어 정보(숫자)만 추출합니다.
         * 예: <FONT color='#...'>진화</FONT> 1티어 <FONT color='#...'>신속 Lv.30</FONT> -> 1
         */
        public String getTier() {
            if (description == null || !description.contains("티어")) {
                return "0";
            }

            int fontEndIndex = description.indexOf("</FONT>");
            int tierIndex = description.indexOf("티어");

            if (fontEndIndex != -1 && tierIndex != -1 && fontEndIndex < tierIndex) {
                // </FONT> 와 "티어" 사이의 텍스트를 추출하여 공백 제거
                return description.substring(fontEndIndex + 7, tierIndex).trim();
            }
            return "0";
        }

        /**
         * Description에서 마지막 <FONT> 태그 내의 내용(효과명 및 레벨)만 추출합니다.
         * 예: <FONT color='#...'>진화</FONT> 1티어 <FONT color='#...'>신속 Lv.30</FONT> -> 신속 Lv.30
         */
        public String getEffectDetail() {
            if (description == null || !description.contains("</FONT>")) {
                return description != null ? description.replace("||", "").trim() : null;
            }

            int lastStart = description.lastIndexOf("<FONT");
            int lastEnd = description.lastIndexOf("</FONT>");

            if (lastStart != -1 && lastEnd != -1) {
                int contentStart = description.indexOf(">", lastStart) + 1;
                if (contentStart > 0 && contentStart < lastEnd) {
                    return description.substring(contentStart, lastEnd).replace("||", "").trim();
                }
            }
            return description.replace("||", "").trim();
        }

        /**
         * 툴팁의 특정 요소를 가져와서 "||" 기호를 제거한 후 반환합니다.
         */
        public String getCleanedToolTipHtml(String key) {
            if (toolTip == null) {
                return "";
            }
            return toolTip.getElementHtml(key).replace("||", "");
        }

        /**
         * getEffectDetail() 결과에서 레벨(Lv.) 정보를 제외한 순수 효과명만 추출합니다.
         * 예: "신속 Lv.30" -> "신속"
         */
        public String getPureName() {
            String detail = getEffectDetail();
            if (detail == null) return "";
            int lvIndex = detail.lastIndexOf("Lv.");
            if (lvIndex != -1) {
                return detail.substring(0, lvIndex).trim();
            }
            return detail;
        }

        /**
         * getEffectDetail() 결과에서 레벨(Lv.) 정보만 추출합니다.
         * 예: "신속 Lv.30" -> "Lv.30"
         */
        public String getPureLevel() {
            String detail = getEffectDetail();
            if (detail == null) return "";
            int lvIndex = detail.lastIndexOf("Lv.");
            if (lvIndex != -1) {
                return detail.substring(lvIndex).trim();
            }
            return "";
        }
    }
}
