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
package com.redhat.devtools.stats.endpoints;

import com.redhat.devtools.stats.services.MarketPlaceService;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

@Path("/api")
public class MarketplaceFacadeResource {

    private final MarketPlaceService service;

    public MarketplaceFacadeResource(MarketPlaceService service) {
        this.service = service;
    }

    @Path("{extensionId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJson(@PathParam String extensionId) {
        JsonObject extensionJson = service.getExtensionData(extensionId);
        if (extensionJson == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(extensionJson).build();
    }
}
