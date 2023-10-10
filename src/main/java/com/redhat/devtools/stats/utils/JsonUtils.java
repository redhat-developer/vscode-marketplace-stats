/*
 * Copyright 2023 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package com.redhat.devtools.stats.utils;

import java.util.Optional;

import com.redhat.devtools.stats.models.Extension;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonUtils {

    public static JsonArray getExtensions(JsonObject publisher) {
        JsonArray results = publisher.getJsonArray("results");
        if (results.isEmpty()) {
            return new JsonArray();
        }
        return results.getJsonObject(0).getJsonArray("extensions").copy();
    }

    public static JsonObject findExtension(String extensionName, JsonObject publisher) {
        return findExtension(extensionName, getExtensions(publisher));
    }

    public static JsonObject findExtension(String extensionName, JsonArray extensions) {
        int dot = extensionName.indexOf(".");
        if (dot > -1) {
            extensionName = extensionName.substring(dot + 1);
        }
        for (int i = 0; i < extensions.size(); i++) {
            JsonObject o = extensions.getJsonObject(i);
            if (extensionName.equals(o.getString("extensionName"))) {
                return o;
            }
        }
        return null;
    }

    /**
     * Returns the statistic value, or 0 if that statistic is not found
     *
     * @return the statistic value, or 0 if that statistic is not found
     */
    public static int getStatAsInt(JsonArray statistics, String name) {
        //@formatter:off
        Optional<JsonObject> statistic = statistics.stream().map(o -> (JsonObject)o)
                                  .filter(stat -> name.equals(stat.getString("statisticName")))
                                  .findFirst();
        if (!statistic.isPresent()) {
            return 0;
        }
        return statistic.map(stat -> stat.getInteger("value")).orElse(0);
        //@formatter:on
    }

    public static Extension marketPlaceToExtension(JsonObject source) {
        Extension extension = new Extension();
        extension.name = source.getJsonObject("publisher").getString("publisherName") + "."
                + source.getString("extensionName");// marketPlaceExtension;
        extension.displayName = source.getString("displayName");

        JsonObject version = source.getJsonArray("versions").getJsonObject(0);
        JsonArray files = version.getJsonArray("files");
        if (!files.isEmpty()) {
            JsonObject file = files.getJsonObject(0);
            extension.icon = file.getString("source");
        }
        return extension;
    }
}
