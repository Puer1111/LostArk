package com.lostark.lostark.model.dto.character.tooltip;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
