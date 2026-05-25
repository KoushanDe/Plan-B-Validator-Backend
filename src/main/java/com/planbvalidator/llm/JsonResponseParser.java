package com.planbvalidator.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public final class JsonResponseParser {

    private JsonResponseParser() {
    }

    public static List<String> stringList(ObjectMapper mapper, JsonNode node, String field) {
        JsonNode array = node.path(field);
        if (array.isMissingNode() || !array.isArray()) {
            return List.of();
        }
        List<String> values = mapper.convertValue(
                array,
                mapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
        return values == null ? List.of() : values.stream().filter(s -> s != null && !s.isBlank()).toList();
    }

    public static String text(JsonNode node, String field, String defaultValue) {
        String value = node.path(field).asText("");
        return value.isBlank() ? defaultValue : value;
    }

    static List<String> emptyIfNull(List<String> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
