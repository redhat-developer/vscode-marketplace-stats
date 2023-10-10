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
package com.redhat.devtools.stats.services;

import com.redhat.devtools.stats.utils.JsonUtils;
import io.quarkus.cache.CacheResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class MarketPlaceService {

    @RestClient
    MarketPlaceRestClient marketPlaceClient;


    @CacheResult(cacheName = "marketplace-api")
    public JsonObject getPublisherData(String publisherId) {
        JsonObject request = createRequest(publisherId);
        return marketPlaceClient.getPublisherData(request);
    }

    public JsonObject getExtensionData(String name) {
        String[] parts = name.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        String publisherId = parts[0];
        String extensionName = parts[1];
        JsonObject publisherJson = getPublisherData(publisherId);
        JsonObject extensionData = JsonUtils.findExtension(extensionName, publisherJson);
        return extensionData;
    }

    private JsonObject createRequest(String name) {
        JsonObject body = new JsonObject();
        JsonArray criteria = new JsonArray();
        criteria.add(new JsonObject().put("filterType", 18).put("value", name));
        criteria.add(new JsonObject().put("filterType", 8).put("value", "Microsoft.VisualStudio.Code"));//
        criteria.add(new JsonObject().put("filterType", 8).put("value", "Microsoft.VisualStudio.Services"));//
        criteria.add(new JsonObject().put("filterType", 8).put("value", "Microsoft.VisualStudio.Services.Cloud"));//
        criteria.add(new JsonObject().put("filterType", 8).put("value", "Microsoft.VisualStudio.Services.Integration"));//
        criteria.add(new JsonObject().put("filterType", 8).put("value", "Microsoft.VisualStudio.Services.Cloud.Integration"));//
        criteria.add(new JsonObject().put("filterType", 8).put("value", "Microsoft.VisualStudio.Services.Resource.Cloud"));//
        criteria.add(new JsonObject().put("filterType", 8).put("value", "Microsoft.TeamFoundation.Server"));//
        criteria.add(new JsonObject().put("filterType", 8).put("value", "Microsoft.TeamFoundation.Server.Integration"));//
        criteria.add(new JsonObject().put("filterType", 12).put("value", "37889"));
        body.put("filters", new JsonArray().add(
                new JsonObject().put("criteria", criteria)
                        .put("sortBy", 4)
                        .put("pageSize", 200)
                        .put("pageNumber", 1)));
        body.put("assetTypes", new JsonArray().add("Microsoft.VisualStudio.Services.Icons.Default"));
        body.put("flags", 866);
        return body;
    }
}
