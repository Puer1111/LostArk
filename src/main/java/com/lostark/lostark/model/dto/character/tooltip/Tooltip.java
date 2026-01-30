package com.lostark.lostark.model.dto.character.tooltip;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Data
public class Tooltip {
    private Map<String, TooltipElement> elements = new HashMap<>();

    @JsonAnySetter
    public void addElement(String name, TooltipElement value) {
        elements.put(name, value);
    }

    public String toHtmlString() {
        if (elements == null || elements.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder("<div>");
        // Sort keys to maintain order
        List<String> sortedKeys = new ArrayList<>(elements.keySet());
        sortedKeys.sort(String::compareTo);

        for (String key : sortedKeys) {
            TooltipElement element = elements.get(key);
            if (element != null) {
                html.append(valueToHtml(element));
            }
        }
        html.append("</div>");
        return html.toString();
    }

    public String getElementHtml(String elementKey) {
        if (elements == null || !elements.containsKey(elementKey)) {
            return "";
        }
        TooltipElement element = elements.get(elementKey);

        if (element == null) {
            return "";
        }
        return valueToHtml(element);
    }

    public String getNestedElementHtml(String topLevelKey, String nestedKey) {
        if (elements == null || !elements.containsKey(topLevelKey)) {
            return "";
        }
        TooltipElement topElement = elements.get(topLevelKey);
        if (topElement == null || !(topElement.getValue() instanceof Map)) {
            return "";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) topElement.getValue();

        if (!nestedMap.containsKey(nestedKey)) {
            return "";
        }

        Object nestedValue = nestedMap.get(nestedKey);

        if (nestedValue instanceof String) {
            String html = (String) nestedValue;
            Document doc = Jsoup.parseBodyFragment(html);
            doc.select("img").remove();
            doc.select("font").removeAttr("size");

            return doc.body().html();
        }

        return "";
    }

    private String valueToHtml(TooltipElement element) {
        Object value = element.getValue();
        if (value == null) {
            return "";
        }

        String type = element.getType();
        StringBuilder elementHtml = new StringBuilder();

        // Wrap elements in a div with a class corresponding to their type
        elementHtml.append("<div class=\"").append(type).append("\">");

        if (value instanceof String) {
            String html = (String) value;
            Document doc = Jsoup.parseBodyFragment(html);
            doc.select("img").remove();
            doc.select("font").removeAttr("size"); // font 에 size 제거
            elementHtml.append(doc.body().html());
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;

            List<String> sortedKeys = new ArrayList<>(map.keySet());
            sortedKeys.sort(String::compareTo);

            for (String innerKey : sortedKeys) {
                Object innerValue = map.get(innerKey);
                if (innerValue instanceof String) {
                    String html = (String) innerValue;
                    Document doc = Jsoup.parseBodyFragment(html);
                    doc.select("img").remove();
                    doc.select("font").removeAttr("size");
                    // Add a class for the inner key if needed for styling
                    elementHtml.append("<div class=\"").append(innerKey).append("\">");
                    elementHtml.append(doc.body().html());
                    elementHtml.append("</div>");
                }
                // Note: This part might need more specific handling if the nested
                // objects (like slotData) need to be rendered differently.
            }
        }
        elementHtml.append("</div>");
        return elementHtml.toString();
    }

    // 상급 재련 값 json 에서 출력해서 프론트로 보내는 메서드.
    public String getAdvancedHoningLevel() {
        if (elements == null || !elements.containsKey("Element_005")) {
            return "";
        }
        TooltipElement element = elements.get("Element_005");
        if (element == null || !(element.getValue() instanceof String)) {
            return "";
        }
        String value = (String) element.getValue();

        // 정규식을 사용하여 상급 재련 단계(숫자) 추출
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<FONT COLOR='#FFD200'>(\\d+)</FONT>단계");
        java.util.regex.Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            return matcher.group(1); // "40"과 같은 숫자 문자열 반환
        }

        return ""; // 값을 찾지 못한 경우
    }

    // 장비  품질 계산
    public String getQualityValue() {
        TooltipElement element = elements.get("Element_001");
        if (element == null || !(element.getValue() instanceof Map)) {
            return "";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> valueMap = (Map<String, Object>) element.getValue();

        Object quality = valueMap.get("qualityValue");
        if (quality instanceof Number) {
            return String.valueOf(((Number) quality).intValue());
        }

        return "";
    }

    public String extractEngravings() {
        // Element_007에 무작위 각인 효과가 있습니다.
        TooltipElement element = elements.get("Element_007");
        if (element == null || element.getValue() == null) {
            return "";
        }

        try {
            // 중첩된 Map 구조를 탐색합니다.
            @SuppressWarnings("unchecked")
            Map<String, Object> valueMap = (Map<String, Object>) element.getValue();

            @SuppressWarnings("unchecked")
            Map<String, Object> indentGroup = (Map<String, Object>) valueMap.get("Element_000");
            if (indentGroup == null || !indentGroup.containsKey("contentStr")) return "";

            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> contentStrMap = (Map<String, Map<String, Object>>) indentGroup.get("contentStr");
            if (contentStrMap == null) return "";

            Map<String, String> engravings = new LinkedHashMap<>();

            // 키를 정렬하여 순서를 보장합니다 (Element_000, Element_001, ...).
            List<String> sortedKeys = new ArrayList<>(contentStrMap.keySet());
            Collections.sort(sortedKeys);

            for (String key : sortedKeys) {
                Map<String, Object> engravingInfo = contentStrMap.get(key);
                if (engravingInfo != null && engravingInfo.containsKey("contentStr")) {
                    String html = (String) engravingInfo.get("contentStr");

                    // 긍정 각인과 레벨을 추출하는 정규식
                    String regex = "\\[<FONT COLOR='#FFFFAC'>(.+?)</FONT>\\].*?Lv\\.(\\d+)";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(html);

                    if (matcher.find()) {
                        String name = matcher.group(1);
                        String level = "Lv" + matcher.group(2); // 예: "Lv" + "3" -> "Lv3"
                        engravings.put(name, level);
                    }
                }
            }

            // 맵을 "각인1 Lv1, 각인2 Lv2" 형태의 문자열로 변환합니다.
            return engravings.entrySet().stream()
                    .map(entry -> entry.getKey() + " " + entry.getValue())
                    .collect(Collectors.joining(", "));

        } catch (ClassCastException e) {
            // 예상치 못한 구조일 경우 예외 처리
            return "";
        }
    }

    public int getGemPoint() {
        TooltipElement element = elements.get("Element_005");
        if (element == null || !(element.getValue() instanceof Map)) {
            return 0;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> valueMap = (Map<String, Object>) element.getValue();
        Object element001 = valueMap.get("Element_001");

        if (!(element001 instanceof String)) {
            return 0;
        }

        String text = (String) element001;
        Pattern pattern = Pattern.compile("<FONT COLOR = '#B7FB00'>(\\d+)</FONT>");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Should not happen if regex matches
            }
        }
        return 0;
    }

    public Map<String, Integer> extractGemEffects() {
        Map<String, Integer> effects = new LinkedHashMap<>(); // Use LinkedHashMap to preserve order

        // Access Element_005
        TooltipElement element005 = elements.get("Element_005");
        if (element005 == null || !(element005.getValue() instanceof Map)) {
            return effects;
        }

        // Access value map within Element_005
        @SuppressWarnings("unchecked")
        Map<String, Object> valueMap = (Map<String, Object>) element005.getValue();

        // Access Element_001 within the value map
        Object element001Value = valueMap.get("Element_001");
        if (!(element001Value instanceof String)) {
            return effects;
        }

        String html = (String) element001Value;
        System.out.println("Extracting Gem Effects from HTML: " + html); // Debug log

        // Regex to find patterns like "[Name]" and "Lv.X"
        // Example: [낙인력] <FONT color='#FFD200'>Lv.4</FONT>
        Pattern combinedPattern = Pattern.compile("\\[([^\\]]+)\\]\\s*<FONT[^>]*>\\s*Lv\\.(\\d+)\\s*</FONT>");
        Matcher matcher = combinedPattern.matcher(html);

        while (matcher.find()) {
            String name = matcher.group(1); // e.g., "낙인력"
            Integer level = Integer.parseInt(matcher.group(2)); // e.g., 4
            effects.put(name, level);
        }

        return effects;
    }

    


}

