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
package org.eclipse.vorto.importers.webofthings;

import java.util.Objects;

public class PropertyBuilder {

    private final Property property;

    public PropertyBuilder() {
        property = new Property();
    }

    public PropertyBuilder name(String name) {
        property.setName(name);
        return this;
    }

    public PropertyBuilder description(String description) {
        property.setDescription(description);
        return this;
    }

    public PropertyBuilder type(String type) {
        if ("string".equalsIgnoreCase(type)) {
            property.setType("string");
        } else if ("integer".equalsIgnoreCase(type)) {
            property.setType("int");
        } else if ("float".equalsIgnoreCase(type)) {
            property.setType("float");
        } else if ("binary".equalsIgnoreCase(type)) {
            property.setType("base64binary");
        } else if ("time".equalsIgnoreCase(type)) {
            property.setType("dateTime");
        } else if ("boolean".equalsIgnoreCase(type)) {
            property.setType("boolean");
        } else {
            property.setType("string");
        }
        return this;
    }

    public PropertyBuilder readonly(Boolean readonly) {
        if (Objects.isNull(readonly)) {
            property.setReadonly(false);
        } else {
            property.setReadonly(readonly);
        }
        return this;
    }

    public Property build() {
        return property;
    }
}
