package com.lostark.lostark.model.dto.character.tooltip;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
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
    // 특정 요소의 하위 내용 뽑기 ( 장신구 )
    @JsonIgnore
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
    @JsonIgnore
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
        java.util.regex.Matcher matcher = pattern.matcher( value);

        if (matcher.find()) {
            return matcher.group(1); // "40"과 같은 숫자 문자열 반환
        }

        return ""; // 값을 찾지 못한 경우
    }

    /**
     * 아크 그리드 젬 효과 조각들을 리스트로 반환 (각각 개별 태그 처리용)
     */
    @JsonIgnore
    public List<String> getSimplifiedArkGridGemList(String elementKey) {
        if (elements == null || !elements.containsKey(elementKey)) return Collections.emptyList();
        TooltipElement element = elements.get(elementKey);
        if (element == null) return Collections.emptyList();

        String html = "";
        Object value = element.getValue();
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.containsKey("Element_001") && map.get("Element_001") instanceof String) {
                html = (String) map.get("Element_001");
            }
        } else if (value instanceof String) {
            html = (String) value;
        }

        if (html == null || html.isEmpty()) return Collections.emptyList();

        List<String> results = new ArrayList<>();

        // 1. 필요 의지력 추출
        Matcher m1 = Pattern.compile("필요 의지력\\s*:\\s*<FONT[^>]*>(\\d+)</FONT>").matcher(html);
        if (m1.find()) results.add("필요 의지력: " + m1.group(1));

        // 2. 혼돈 또는 질서 포인트 추출
        Matcher m2 = Pattern.compile("(혼돈|질서) 포인트\\s*:\\s*<FONT[^>]*>(\\d+)</FONT>").matcher(html);
        if (m2.find()) results.add(m2.group(1) + " 포인트: " + m2.group(2));

        // 3. [이름] 및 Lv.값 추출 (대소문자 무관, 공백 유연하게 매칭)
        Matcher m3 = Pattern.compile("\\[?([^\\[\\]<\\s]+(?:\\s+[^\\[\\]<\\s]+)*)\\]?\\s*(?:<FONT[^>]*>)?([lL][vV]\\.?\\s*\\d+)(?:</FONT>)?").matcher(html);
        while (m3.find()) {
            String effect = "[" + m3.group(1).trim() + "] " + m3.group(2).replaceAll("\\s+", "");
            results.add(effect);
        }

        return results;
    }

    // 장비  품질 계산
    @JsonIgnore
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

    // 어빌리티 스톤 에서 각인 이름 추출.
    @JsonIgnore
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

    @JsonIgnore
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

    @Data
    public static class GemsTooltipData {
        private List<String> skillEffects = new ArrayList<>(); // 스킬 효과 (예: 피해 증가, 재사용 대기시간 감소)
        private List<String> additionalEffects = new ArrayList<>(); // 추가 효과 (예: 기본 공격력 증가)
    }
    @JsonIgnore
    public GemsTooltipData getGemsTooltip() {

        TooltipElement element006 = elements.get("Element_006");

        if (element006 == null || !"ItemPartBox".equals(element006.getType())) {

            return null;

        }
        Object value = element006.getValue();

        if (!(value instanceof Map)) {

            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> itemPartBox = (Map<String, String>) value;
                String descriptionHtml = itemPartBox.get("Element_001");
                if (descriptionHtml == null) {
                    return null;
                }
        
                GemsTooltipData data = new GemsTooltipData();
        
                String additionalEffectDelimiter = "<FONT COLOR='#A9D0F5'>추가 효과</FONT>";
                int delimiterIndex = descriptionHtml.indexOf(additionalEffectDelimiter);
        
                String skillEffectPart;
                String additionalEffectPart = null;
        
                if (delimiterIndex != -1) {
                    skillEffectPart = descriptionHtml.substring(0, delimiterIndex);
                    int additionalEffectStart = delimiterIndex + additionalEffectDelimiter.length(); // + "<BR>".length()는 parseAndAddEffects에서 처리
                    if (additionalEffectStart < descriptionHtml.length()) {
                        additionalEffectPart = descriptionHtml.substring(additionalEffectStart);
                    }
                } else {
                    skillEffectPart = descriptionHtml;
                }
        
                // 스킬 효과 파싱 (클래스명 제거)
                parseAndAddEffects(skillEffectPart, data.getSkillEffects(), true);
                // 추가 효과 파싱 (존재하는 경우, 클래스명 제거 안 함)
                if (additionalEffectPart != null) {
                    parseAndAddEffects(additionalEffectPart, data.getAdditionalEffects(), false);
                }
        
                return data;



    }

    @JsonIgnore
    public List<String> getFilteredGemsTooltip() {
        GemsTooltipData rawGemsTooltipData = getGemsTooltip();
        if (rawGemsTooltipData == null || rawGemsTooltipData.getSkillEffects() == null) {
            return Collections.emptyList();
        }

        List<String> filteredList = new ArrayList<>();
        // 정규 표현식: 숫자.숫자% 증가 또는 감소 패턴을 찾습니다.
        // 예를 들어 "22.00% 감소", "1.00% 증가"와 같은 패턴을 찾습니다.
        Pattern pattern = Pattern.compile("(\\d+\\.\\d+|\\d+)%\\s*(증가|감소)");

        for (String tooltip : rawGemsTooltipData.getSkillEffects()) {
            Matcher matcher = pattern.matcher(tooltip);
            if (matcher.find()) { // 패턴과 일치하는 부분이 있다면
                filteredList.add(tooltip);
            }
        }

        // Sort the filteredList: "증가" items before "감소" items
        Collections.sort(filteredList, (s1, s2) -> {
            boolean s1IsIncrease = s1.contains("증가");
            boolean s2IsIncrease = s2.contains("증가");

            if (s1IsIncrease && !s2IsIncrease) { // s1이 증가이고 s2가 증가가 아니면 (즉 감소이면) s1이 먼저
                return -1;
            } else if (!s1IsIncrease && s2IsIncrease) { // s2가 증가이고 s1이 증가가 아니면 (즉 감소이면) s2가 먼저
                return 1;
            }
            // 둘 다 증가이거나 둘 다 감소인 경우 (정규식 필터링으로 인해 이 외의 경우는 없음), 기존 순서 유지
            return 0;
        });

        return filteredList;
    }

    @JsonIgnore
    public String getPrimaryEffectType() {
        List<String> tips = getFilteredGemsTooltip();
        if (tips == null || tips.isEmpty()) {
            return "NONE";
        }
        // The first tip is considered primary due to the existing sort logic in getFilteredGemsTooltip
        String firstTip = tips.get(0);
        if (firstTip.contains("증가")) {
            return "INCREASE";
        }
        if (firstTip.contains("감소")) {
            return "DECREASE";
        }
        return "NONE";
    }

    /**
     * 보석의 효과를 확인하여 "피해" 또는 "감소"로 단순화된 이름을 반환합니다.
     */
    @JsonIgnore
    public String getSimplifiedGemName() {
        String type = getPrimaryEffectType();
        if ("INCREASE".equals(type)) {
            return "피해";
        }
        if ("DECREASE".equals(type)) {
            return "감소";
        }
        return getCleanedGemName(); // 판별 불가 시 기존 이름 반환
    }




    /**
     * 보석 레벨에 따라 CSS 클래스명을 반환합니다.
     */
    @JsonIgnore
    public String getGemLevelClass(int level) {
        if (level >= 10) {
            return "gem-border-10"; // 10레벨
        } else if (level >= 8) {
            return "gem-border-8";  // 8~9레벨
        } else {
            return "gem-border-base"; // 7레벨 이하
        }
    }


    private void parseAndAddEffects(String htmlPart, List<String> targetList, boolean removeClassName) {
        if (htmlPart == null || htmlPart.trim().isEmpty()) {
            return;
        }
        String cleanedDescription = htmlPart;
        if (removeClassName) {
            // [클래스명] 제거 (예: [아르카나])
            cleanedDescription = cleanedDescription.replaceAll("\\[[^\\]]+]\s*", "");
        }
        // <FONT> 태그 제거
        cleanedDescription = cleanedDescription.replaceAll("<FONT[^>]*>", "")
                                .replaceAll("</FONT>", "");
        String[] parts = cleanedDescription.split("<BR>");
        for (String part : parts) {
            String trimmedPart = part.trim();
            // "추가 효과" 텍스트 자체는 리스트에 추가하지 않음
            if (!trimmedPart.isEmpty() && !trimmedPart.equals("추가 효과")) {
                targetList.add(trimmedPart);
            }
        }
    }


    /**
     * Tooltip의 Element_000에 있는 HTML 문자열에서 보석 이름을 추출하여 반환합니다.
     *
     * <p>
     * <p>
     * 예: "<P ALIGN='CENTER'><FONT COLOR='#F99200'>6레벨 광휘의 보석 (귀속)</FONT></P>" -> "6레벨 광휘의 보석 (귀속)"
     *
     * @return 추출된 보석 이름. HTML이 유효하지 않거나 텍스트를 찾을 수 없는 경우 빈 문자열 반환.
     */

    @JsonIgnore
    public String getCleanedGemName() {

        TooltipElement element = elements.get("Element_000");

        if (element == null || !(element.getValue() instanceof String)) {

            return "";

        }

        String htmlString = (String) element.getValue();


        if (htmlString.isEmpty()) {

            return "";

        }

        // Regex to extract content within <FONT> tags, ignoring any outer HTML

        Pattern pattern = Pattern.compile(".*?<FONT[^>]*>(.*?)</FONT>.*", Pattern.DOTALL);

        Matcher matcher = pattern.matcher(htmlString);


        if (matcher.find()) {

            return matcher.group(1);

        }


        // If <FONT> tags are not found, try to extract from <P> tags as a fallback

        pattern = Pattern.compile(".*?<P[^>]*>(.*?)</P>.*", Pattern.DOTALL);

        matcher = pattern.matcher(htmlString);

        if (matcher.find()) {

            return matcher.group(1);

        }


        return htmlString; // If no tags found, return original string as a last resort

    }

    /**
     * 스킬 툴팁에서 무력화, 부위파괴, 면역 정보를 추출합니다.
     */
    @JsonIgnore
    public List<String> extractSkillAttributes() {
        Set<String> attributes = new LinkedHashSet<>();
        if (elements == null) return new ArrayList<>();

        for (TooltipElement element : elements.values()) {
            searchAttributesInObject(element.getValue(), attributes);
        }
        return new ArrayList<>(attributes);
    }

    private void searchAttributesInObject(Object value, Set<String> attributes) {
        if (value instanceof String) {
            String text = Jsoup.parse((String) value).text();

            // 1. 무력화 (예: 무력화 : [중상])
            if (text.contains("무력화")) {
                Pattern p = Pattern.compile("무력화\\s*:\\s*\\[.*?\\]");
                Matcher m = p.matcher(text);
                if (m.find()) attributes.add(m.group().trim());
            }

            // 2. 부위파괴 (예: 부위파괴 : 레벨 1)
            if (text.contains("부위파괴")) {
                Pattern p = Pattern.compile("부위파괴\\s*:\\s*레벨\\s*\\d+");
                Matcher m = p.matcher(text);
                if (m.find()) attributes.add(m.group().trim());
            }

            // 3. 면역 효과 (트라이포드 '강인함' 포함)
            if (text.contains("경직 면역")) attributes.add("경직 면역");
            if (text.contains("피격 면역") || text.contains("강인함")) attributes.add("피격 면역");
            if (text.contains("상태 이상 면역")) attributes.add("상태 이상 면역");

            // 4. 카운터 가능 여부
            if (text.contains("카운터 : 가능")) attributes.add("카운터");

        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            for (Object v : map.values()) {
                searchAttributesInObject(v, attributes);
            }
        }
    }

}

    

