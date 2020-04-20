/**
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 * <p>
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 * <p>
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.vorto.codegen.webofthings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.vorto.core.api.model.datatype.Property;
import org.eclipse.vorto.core.api.model.functionblock.*;
import org.eclipse.vorto.core.api.model.informationmodel.InformationModel;
import org.eclipse.vorto.plugin.generator.GeneratorPluginInfo;
import org.eclipse.vorto.plugin.generator.ICodeGenerator;
import org.eclipse.vorto.plugin.generator.IGenerationResult;
import org.eclipse.vorto.plugin.generator.InvocationContext;
import org.eclipse.vorto.plugin.generator.adapter.ObjectMapperFactory;
import org.eclipse.vorto.plugin.generator.utils.Generated;
import org.eclipse.vorto.plugin.generator.utils.SingleGenerationResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class WebOfThingsGenerator implements ICodeGenerator {

    private final String version;

    private static final String KEY = "webofthings";

    private static final String DESCRIPTION = "description";

    private static final Function<FunctionBlock, List<Event>> EVENT_FUNCTION =
        FunctionBlock::getEvents;

    private static final Function<FunctionBlock, Status> STATUS_FUNCTION = FunctionBlock::getStatus;

    private static final Function<FunctionBlock, List<Operation>> OPERATION_FUNCTION =
        FunctionBlock::getOperations;


    public WebOfThingsGenerator() {
        version = loadVersionFromResources();
    }

    @Override
    public IGenerationResult generate(InformationModel model, InvocationContext context) {
        ObjectNode thingTemplate = newObjectNode();
        ArrayNode contextNode = new ArrayNode(new JsonNodeFactory(false));
        contextNode.add("https://www.w3.org/2019/wot/td/v1");
        contextNode.add("https://vorto.eclipse.org");
        thingTemplate.set("@context", contextNode);
        thingTemplate.put("@type", "ThingTemplate");
        thingTemplate.put("title", model.getName() + " Thing Description Template");

        addPropertiesNode(model, thingTemplate);
        addActionsNode(model, thingTemplate);
        addEventsNode(model, thingTemplate);

        try {
            return createResult(ObjectMapperFactory.getInstance().writerWithDefaultPrettyPrinter().writeValueAsString(thingTemplate));
        } catch (JsonProcessingException e) {
            return createResult(thingTemplate.toString());
        }
    }

    private void addPropertiesNode(InformationModel model, ObjectNode thingTemplate) {
        ObjectNode properties = newObjectNode();
        Consumer<Status> addNodeForEachStatusConsumer = status -> status.getProperties()
            .forEach(statusProperty -> addNodeForEachStatus(properties, statusProperty));
        processModel(model, STATUS_FUNCTION, addNodeForEachStatusConsumer);
        thingTemplate.set("properties", properties);
    }

    private void addEventsNode(InformationModel model, ObjectNode thingTemplate) {
        ObjectNode events = newObjectNode();
        processModel(model, EVENT_FUNCTION,
            fbEvents -> fbEvents.forEach(event -> addEventNode(events, event)));
        thingTemplate.set("events", events);
    }

    private void addEventNode(ObjectNode events, Event event) {
        ObjectNode eventNode = newObjectNode();
        String eventName = event.getName();
        event.getProperties().forEach(
            eventProperty -> addEventPropertiesToNode(eventNode, eventProperty));
        events.set(eventName, eventNode);
    }

    private void addEventPropertiesToNode(ObjectNode eventNode, Property eventProperty) {
        ObjectNode eventPropertyNode = newObjectNode();
        eventProperty.getConstraintRule().getConstraints().forEach(
            eventConstraint -> putIfNotNull(eventPropertyNode,
                eventConstraint.getType().getName(),
                eventConstraint.getConstraintValues()));
        putIfNotNull(eventPropertyNode, DESCRIPTION, eventProperty.getDescription());
        eventNode.set(eventProperty.getName(), eventPropertyNode);
    }

    private void addActionsNode(InformationModel model, ObjectNode thingTemplate) {
        ObjectNode actions = newObjectNode();
        Consumer<List<Operation>> addNodeForEachOperationConsumer = operations -> operations
            .forEach(operation -> addNodeForEachOperation(actions, operation));
        processModel(model, OPERATION_FUNCTION, addNodeForEachOperationConsumer);
        thingTemplate.set("actions", actions);
    }

    private void addNodeForEachOperation(ObjectNode actions, Operation operation) {
        ObjectNode operationNode = newObjectNode();
        putIfNotNull(operationNode, DESCRIPTION, operation.getDescription());
        putIfNotNull(operationNode, "breakable", String.valueOf(operation.isBreakable()));
        putIfNotNull(operationNode, "extension", String.valueOf(operation.isExtension()));
        actions.set(operation.getName(), operationNode);
    }

    private void addNodeForEachStatus(ObjectNode properties, Property statusProperty) {
        ObjectNode statusPropertyNode = newObjectNode();
        statusProperty.getConstraintRule().getConstraints().forEach(
            statusConstraint -> putIfNotNull(statusPropertyNode,
                statusConstraint.getType().getName(), statusConstraint.getConstraintValues()));
        putIfNotNull(statusPropertyNode, DESCRIPTION, statusProperty.getDescription());
        putIfNotNull(statusPropertyNode, "mandatory",
            String.valueOf(statusProperty.getPresence().isMandatory()));
        properties.set(statusProperty.getName(), statusPropertyNode);
    }

    @Override
    public GeneratorPluginInfo getMeta() {
        return GeneratorPluginInfo.Builder(KEY)
            .withDescription("Generates the Web of Things Thing Description Template")
            .withName("Web of Things")
            .withVendor("Eclipse Vorto Team")
            .withPluginVersion(version)
            .build();
    }

    private IGenerationResult createResult(String result) {
        Generated generated = new Generated("thingdefinitiontemplate.json", "/", result);
        SingleGenerationResult generationResult = new SingleGenerationResult("application/json");
        generationResult.write(generated);
        return generationResult;
    }

    private void putIfNotNull(ObjectNode node, String name, String value) {
        if (Objects.nonNull(value))
            node.put(name, value);
    }

    private ObjectNode newObjectNode() {
        return ObjectMapperFactory.getInstance().createObjectNode();
    }

    private <T> void processModel(InformationModel model, Function<FunctionBlock, T> function,
        Consumer<T> consumer) {
        model.getProperties().forEach(
            functionblockProperty -> Optional.ofNullable(functionblockProperty.getType())
                .map(FunctionblockModel::getFunctionblock)
                .map(function)
                .ifPresent(consumer));
    }
}
