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

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.vorto.core.api.model.functionblock.FunctionBlock;
import org.eclipse.vorto.core.api.model.informationmodel.InformationModel;
import org.eclipse.vorto.plugin.generator.GeneratorPluginInfo;
import org.eclipse.vorto.plugin.generator.ICodeGenerator;
import org.eclipse.vorto.plugin.generator.IGenerationResult;
import org.eclipse.vorto.plugin.generator.InvocationContext;
import org.eclipse.vorto.plugin.generator.utils.Generated;
import org.eclipse.vorto.plugin.generator.utils.SingleGenerationResult;
import org.json.JSONObject;
import org.mozilla.iot.webthing.Action;
import org.mozilla.iot.webthing.Event;
import org.mozilla.iot.webthing.Property;
import org.mozilla.iot.webthing.Thing;
import org.mozilla.iot.webthing.Value;

public class WebOfThingsGenerator implements ICodeGenerator {

  private final String version;

  public WebOfThingsGenerator() {
    version = loadVersionFromResources();
  }

  @Override
  public IGenerationResult generate(InformationModel model, InvocationContext context) {
    Thing webThing = new Thing("urn:" +
        model.getNamespace() + ":" + model.getName() + ":" + model.getVersion(),
        model.getDisplayname());

    model.getProperties().forEach(fb -> {
        Optional<FunctionBlock> extendedFunctionBlock = Optional.ofNullable(fb.getExtendedFunctionBlock());
      Value<String> v = new Value("");
      Property<String> p = new Property<>(webThing, fb.getType().getNamespace() + ":" + fb.getName() + ":" + fb.getType().getVersion(), v);
      webThing.addProperty(p);

      extendedFunctionBlock.ifPresent(efb -> {
        Optional.ofNullable(efb.getEvents())
            .ifPresent(events -> events
                .stream()
                .filter(Objects::nonNull)
                .forEach(e -> webThing.addEvent(new Event<>(webThing, e.getName(), new ArrayList<>(e.getProperties())))));
        Optional.ofNullable(efb.getOperations())
            .ifPresent(operations -> operations
                .stream()
                .filter(Objects::nonNull)
                .forEach(o -> {
                  JSONObject metaData = new JSONObject();
                  metaData.put("description", o.getDescription());
                  metaData.put("label", o.getName());
                  metaData.put("params", "TODO");
                  webThing.addAvailableAction(o.getName(), metaData, Action.class);
                }));
        Optional.ofNullable(efb.getStatus()).ifPresent(status -> status.getProperties()
            .stream()
            .filter(Objects::nonNull)
            .forEach(statusProperty -> {
              // TODO
        }));
      });
    });

    Generated generated = new Generated("thingdefinition.json", "/",
        webThing.asThingDescription().toString().getBytes());
    SingleGenerationResult generationResult = new SingleGenerationResult("application/json");
    generationResult.write(generated);
    return generationResult;
  }

  @Override
  public GeneratorPluginInfo getMeta() {
    return GeneratorPluginInfo.Builder("webofthings")
        .withDescription(
            "Generates the Web of Things Thing Description")
        .withName("Web of Things")
        .withVendor("Eclipse Vorto Team")
        .withPluginVersion(version)
        .build();
  }
}
