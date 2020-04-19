package org.eclipse.vorto.importers.webofthings;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WebOfThingsImporter {

    public String convertToVorto(String thingDescription, String namespace, String version) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.reader().readTree(thingDescription);
        String title = jsonNode.get("title").asText();
        String description = jsonNode.get("description").asText();

        List<Property> propertyList = new ArrayList<>();
        jsonNode.get("properties").getFieldNames().forEachRemaining(fieldName -> {
            JsonNode child = jsonNode.get("properties").get(fieldName);
            propertyList.add(new PropertyBuilder().name(fieldName)
                    .description(parseString(child.get("description")))
                    .readonly(parseBoolean(child.get("readOnly")))
                    .type(parseString(child.get("type")))
                    .build());
        });

        List<Property> actionList = new ArrayList<>();
        jsonNode.get("actions").getFieldNames().forEachRemaining(fieldName -> {
            JsonNode child = jsonNode.get("actions").get(fieldName);
            actionList.add(new PropertyBuilder().name(fieldName)
                    .description(parseString(child.get("description")))
                    .readonly(parseBoolean(child.get("readOnly")))
                    .type(parseString(child.get("type")))
                    .build());
        });

        List<Property> events = new ArrayList<>();
        jsonNode.get("events").getFieldNames().forEachRemaining(fieldName -> {
            JsonNode child = jsonNode.get("events").get(fieldName);
            events.add(new PropertyBuilder().name(fieldName)
                    .description(parseString(child.get("description")))
                    .readonly(parseBoolean(child.get("readOnly")))
                    .type(parseString(child.get("type")))
                    .build());
        });
        return new FunctionblockTemplate().create(namespace, version, title, description, propertyList, actionList, events);
    }

    private Boolean parseBoolean(JsonNode node) {
        if (Objects.isNull(node)) {
            return null;
        }
        return Boolean.parseBoolean(node.toString());
    }

    private String parseString(JsonNode node) {
        if (Objects.isNull(node)) {
            return null;
        }
        return node.asText();
    }
}
