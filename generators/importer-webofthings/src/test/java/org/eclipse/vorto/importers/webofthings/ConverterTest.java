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

import org.junit.Test;

import java.io.IOException;

public class ConverterTest {

    private final String JSON = "{\n" +
            "    \"@context\": [\"https://www.w3.org/2019/wot/td/v1\"], \n" +
            "    \"@type\" : \"ThingTemplate\",\n" +
            "    \"title\": \"Lamp Thing Description Template\",\n" +
            "    \"description\" : \"Lamp Thing Description Template\",\n" +
            "    \"properties\": {\n" +
            "        \"lampStatus\": {\n" +
            "            \"description\" : \"current status of the lamp (on|off)\",\n" +
            "            \"type\": \"string\",\n" +
            "            \"readOnly\": true\n" +
            "        }\n" +
            "    },\n" +
            "    \"actions\": {\n" +
            "        \"toggle\": {\n" +
            "            \"description\" : \"Turn the lamp on or off\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"events\": {\n" +
            "        \"overheating\": {\n" +
            "            \"description\" : \"Lamp reaches a critical temperature (overheating)\",\n" +
            "            \"data\": {\"type\": \"string\"}\n" +
            "        }\n" +
            "    }\n" +
            "}";

    @Test
    public void test() throws IOException {
        WebOfThingsImporter webOfThingsImporter = new WebOfThingsImporter();
        System.out.println(webOfThingsImporter.convertToVorto(JSON, "org.test.vorto", "1.0.0"));
    }
}
