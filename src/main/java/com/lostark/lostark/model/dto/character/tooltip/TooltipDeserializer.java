package com.lostark.lostark.model.dto.character.tooltip;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * 로스트아크 툴팁 데이터를 위한 커스텀 역직렬화기
 * API 응답(String 형태의 JSON)과 캐시 데이터(Object 형태의 JSON)를 모두 지원하며,
 * 규격 외 필드로 인한 오류를 방어합니다.
 */
public class TooltipDeserializer extends JsonDeserializer<Tooltip> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public Tooltip deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();

        if (node == null || node.isNull()) {
            return null;
        }

        // 1. Tooltip이 이미 JSON 객체 형태인 경우 (예: Redis 캐시에서 로드된 경우)
        if (node.isObject()) {
            return mapNodeToTooltip(node);
        }

        // 2. Tooltip이 문자열 형태인 경우 (로스트아크 API 원본 응답 또는 Raw HTML)
        if (node.isTextual()) {
            String text = node.asText().trim();
            if (text.isEmpty()) {
                return null;
            }

            // JSON 객체 문자열인지 확인 ({ } 로 감싸져 있는지)
            if (text.startsWith("{") && text.endsWith("}")) {
                try {
                    JsonNode nestedNode = objectMapper.readTree(text);
                    if (nestedNode.isObject()) {
                        return mapNodeToTooltip(nestedNode);
                    }
                } catch (IOException e) {
                    // JSON 파싱 실패 시 일반 텍스트(Raw HTML)로 간주하고 진행
                }
            }

            // JSON이 아니거나 파싱에 실패한 경우 Raw HTML 툴팁 생성
            return createRawHtmlTooltip(text);
        }

        return null;
    }

    /**
     * JsonNode의 필드들을 분석하여 Tooltip 객체를 구성합니다.
     * "Element_XXX" 패턴의 필드만 수용하여 simplifiedGemName 등 비규격 필드로 인한 오류를 차단합니다.
     */
    private Tooltip mapNodeToTooltip(JsonNode node) {
        Tooltip tooltip = new Tooltip();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();

            // 로스트아크 툴팁 데이터는 항상 "Element_"로 시작하는 필드 구조를 가짐
            // 그 외의 필드(예: 직렬화 과정에서 추가된 계산용 속성 등)는 무시하여 역직렬화 안정성 확보
            if (key != null && key.startsWith("Element_")) {
                try {
                    TooltipElement element = objectMapper.treeToValue(value, TooltipElement.class);
                    if (element != null) {
                        tooltip.addElement(key, element);
                    }
                } catch (JsonProcessingException e) {
                    // 특정 요소의 구조가 깨져있더라도 해당 요소만 건너뛰고 전체 복구는 유지 (Defensive)
                }
            }
        }
        return tooltip;
    }

    /**
     * 파싱할 수 없는 문자열이나 순수 HTML 내용을 가진 기본 툴팁 객체를 생성합니다.
     */
    private Tooltip createRawHtmlTooltip(String text) {
        Tooltip tooltip = new Tooltip();
        TooltipElement element = new TooltipElement();
        element.setType("RawHtml");
        element.setValue(text);
        tooltip.addElement("Element_000", element);
        return tooltip;
    }
}
