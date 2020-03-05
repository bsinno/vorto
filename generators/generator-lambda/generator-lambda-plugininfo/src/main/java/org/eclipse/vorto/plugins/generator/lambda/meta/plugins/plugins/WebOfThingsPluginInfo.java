/**
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.vorto.plugins.generator.lambda.meta.plugins.plugins;

import org.eclipse.vorto.codegen.webofthings.WebOfThingsGenerator;
import org.eclipse.vorto.plugin.generator.GeneratorPluginInfo;

public class WebOfThingsPluginInfo implements IPluginMeta {

  private GeneratorPluginInfo info;

  public WebOfThingsPluginInfo() {
    WebOfThingsGenerator wot = new WebOfThingsGenerator();
    info = wot.getMeta();
  }

  @Override
  public GeneratorPluginInfo getInfo() {
    return info;
  }

}
