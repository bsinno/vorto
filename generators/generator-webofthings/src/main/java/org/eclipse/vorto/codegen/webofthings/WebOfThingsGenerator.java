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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.vorto.core.api.model.datatype.ConstraintRule;
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
        thingTemplate.put("@context", "https://www.w3.org/2019/wot/td/v1");
        thingTemplate.put("@type", "ThingTemplate");
        thingTemplate.put("title", model.getName() + " Thing Description Template");

        addPropertiesNode(model, thingTemplate);
        addActionsNode(model, thingTemplate);
        addEventsNode(model, thingTemplate);

        return createResult(thingTemplate.toString());
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
            eventProperty -> eventProperty.getConstraintRule().getConstraints().forEach(
                eventConstraint -> putIfNotNull(eventNode,
                    eventConstraint.getType().getName(),
                    eventConstraint.getConstraintValues())));
        events.set(eventName, eventNode);
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
        String operationName = operation.getName();
        String operationDescription = operation.getDescription();
        boolean operationBreakable = operation.isBreakable();
        boolean operationExtension = operation.isExtension();

        putIfNotNull(operationNode, DESCRIPTION, operationDescription);
        putIfNotNull(operationNode, "breakable", String.valueOf(operationBreakable));
        putIfNotNull(operationNode, "extension", String.valueOf(operationExtension));
        actions.set(operationName, operationNode);
    }

    private void addNodeForEachStatus(ObjectNode properties, Property statusProperty) {
        ObjectNode statusPropertyNode = newObjectNode();
        String statusName = statusProperty.getName();
        String statusDescription = statusProperty.getDescription();
        boolean isStatusMandatory = statusProperty.getPresence().isMandatory();
        ConstraintRule statusConstraintRule = statusProperty.getConstraintRule();
        statusConstraintRule.getConstraints().forEach(
            statusConstraint -> putIfNotNull(statusPropertyNode,
                statusConstraint.getType().getName(), statusConstraint.getConstraintValues()));
        statusProperty.getPropertyAttributes().forEach(statusPropertyAttribute -> {
            String statusPropertyAtttributeString = statusPropertyAttribute.toString();
        });
        putIfNotNull(statusPropertyNode, DESCRIPTION, statusDescription);
        putIfNotNull(statusPropertyNode, "mandatory", String.valueOf(isStatusMandatory));
        properties.set(statusName, statusPropertyNode);
    }

    private IGenerationResult createResult(String result) {
        Generated generated = new Generated("thingdefinitiontemplate.json", "/", result);
        SingleGenerationResult generationResult = new SingleGenerationResult("application/json");
        generationResult.write(generated);
        return generationResult;
    }

    @Override
    public GeneratorPluginInfo getMeta() {
        return GeneratorPluginInfo.Builder(KEY)
            .withDescription("Generates the Web of Things Thing Description Template")
            .withName("Web of Things").withVendor("Eclipse Vorto Team").withPluginVersion(version)
            .build();
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
