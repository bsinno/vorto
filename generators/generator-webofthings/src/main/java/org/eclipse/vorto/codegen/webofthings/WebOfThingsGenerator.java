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
import org.eclipse.vorto.core.api.model.functionblock.FunctionBlock;
import org.eclipse.vorto.core.api.model.functionblock.FunctionblockModel;
import org.eclipse.vorto.core.api.model.informationmodel.InformationModel;
import org.eclipse.vorto.plugin.generator.GeneratorPluginInfo;
import org.eclipse.vorto.plugin.generator.ICodeGenerator;
import org.eclipse.vorto.plugin.generator.IGenerationResult;
import org.eclipse.vorto.plugin.generator.InvocationContext;
import org.eclipse.vorto.plugin.generator.adapter.ObjectMapperFactory;
import org.eclipse.vorto.plugin.generator.utils.Generated;
import org.eclipse.vorto.plugin.generator.utils.SingleGenerationResult;

import java.util.Objects;
import java.util.Optional;

public class WebOfThingsGenerator implements ICodeGenerator {

    private final String version;

    public WebOfThingsGenerator() {
        version = loadVersionFromResources();
    }

    @Override public IGenerationResult generate(InformationModel model, InvocationContext context) {
        String result = null;
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
        model.getProperties().forEach(functionblockProperty -> {

            String functionblockName = functionblockProperty.getName();
            String functionblockDescription = functionblockProperty.getDescription();
            if (Objects.nonNull(functionblockProperty.getPresence())) {
                boolean isFunctionblockMandatory =
                    functionblockProperty.getPresence().isMandatory();
            }
            boolean isFunctionblockMultiplicity = functionblockProperty.isMultiplicity();


            Optional.ofNullable(functionblockProperty.getType())
                .map(FunctionblockModel::getFunctionblock).map(FunctionBlock::getStatus).ifPresent(
                status -> status.getProperties()
                    .forEach(statusProperty -> addStatusNode(properties, statusProperty)));
        });
        thingTemplate.set("properties", properties);
    }

    private void addEventsNode(InformationModel model, ObjectNode thingTemplate) {
        ObjectNode events = newObjectNode();
        model.getProperties().forEach(
            functionblockProperty -> Optional.ofNullable(functionblockProperty.getType())
                .map(FunctionblockModel::getFunctionblock).map(FunctionBlock::getEvents)
                .ifPresent(fbEvents -> fbEvents.forEach(event -> {
                    ObjectNode eventNode = newObjectNode();
                    String eventName = event.getName();
                    event.getProperties().forEach(eventProperty -> {
                        String eventPropertyName = eventProperty.getName();
                        String eventPropertyDescription = eventProperty.getDescription();
                        eventProperty.getConstraintRule().getConstraints().forEach(
                            eventConstraint -> putIfNotNull(eventNode,
                                eventConstraint.getType().getName(),
                                eventConstraint.getConstraintValues()));
                    });
                    events.set(eventName, eventNode);
                })));
        thingTemplate.set("events", events);
    }

    private void addActionsNode(InformationModel model, ObjectNode thingTemplate) {
        ObjectNode actions = newObjectNode();
        model.getProperties().forEach(functionblockProperty -> {
            Optional.ofNullable(functionblockProperty.getType())
                .map(FunctionblockModel::getFunctionblock).map(FunctionBlock::getOperations)
                .ifPresent(operations -> operations.forEach(operation -> {
                    ObjectNode operationNode = newObjectNode();
                    String operationName = operation.getName();
                    String operationDescription = operation.getDescription();
                    boolean operationBreakable = operation.isBreakable();
                    boolean operationExtension = operation.isExtension();

                    putIfNotNull(operationNode, "description", operationDescription);
                    putIfNotNull(operationNode, "breakable", String.valueOf(operationBreakable));
                    putIfNotNull(operationNode, "extension", String.valueOf(operationExtension));
                    actions.set(operationName, operationNode);
                }));
        });
        thingTemplate.set("actions", actions);
    }

    private void addStatusNode(ObjectNode properties, Property statusProperty) {
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
        putIfNotNull(statusPropertyNode, "description", statusDescription);
        putIfNotNull(statusPropertyNode, "mandatory", String.valueOf(isStatusMandatory));
        properties.set(statusName, statusPropertyNode);
    }

    private IGenerationResult createResult(String result) {
        Generated generated = new Generated("thingdefinition.json", "/", result);
        SingleGenerationResult generationResult = new SingleGenerationResult("application/json");
        generationResult.write(generated);
        return generationResult;
    }

    @Override public GeneratorPluginInfo getMeta() {
        return GeneratorPluginInfo.Builder("webofthings")
            .withDescription("Generates the Web of Things Thing Description")
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
}
