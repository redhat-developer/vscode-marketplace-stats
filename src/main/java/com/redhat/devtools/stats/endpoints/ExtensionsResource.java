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

import com.redhat.devtools.stats.models.Extension;
import com.redhat.devtools.stats.models.ExtensionInstall;
import com.redhat.devtools.stats.services.MarketPlaceStatisticsWatcher;
import io.quarkus.logging.Log;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

@Path("/")
public class ExtensionsResource {

    private static final List<String> VALID_STATS_PARAM_VALUES = List.of("time", "delta", "installs", "updates",
            "total_installed", "onpremDownloads");
    private static final String VALID_STATS_PARAM_MSG = "Valid values for the stats parameter are: "
            + VALID_STATS_PARAM_VALUES;

    @Inject
    private MarketPlaceStatisticsWatcher watcher;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("index.html")
    public TemplateInstance index() {
        long start = System.currentTimeMillis();
        List<Extension> extensions = Extension.findActive();
        Log.infov("Fetched data in {0} ms", System.currentTimeMillis() - start);
        return Templates.index(extensions);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return index();
    }

    @GET
    @Path("{extensionId}.html")
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(@PathParam String extensionId) {
        Extension extension = getExtension(extensionId);
        return Templates.details(extension);
    }

    @GET
    @Path("stats/{extensionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stats(@PathParam String extensionId, @Context UriInfo uriInfo) {
        List<String> columns = getColumns(uriInfo);
        Extension extension = getExtension(extensionId);

        Map<String, VersionStats> versionStatsMap = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        Stream<ExtensionInstall> data = ExtensionInstall.getFrom(extension);
        Log.infov("Fetched stats in {0} ms", System.currentTimeMillis() -start);

        start = System.currentTimeMillis();
        data.forEach(ei -> {
            VersionStats stats = versionStatsMap.computeIfAbsent(ei.version, VersionStats::new);
            Map<String, Object> eventData = new HashMap<>();
            columns.forEach(col -> eventData.put(col, getStat(col, ei)));
            stats.events.add(eventData);
        });
        Log.infov("Organized stats in {0} ms",System.currentTimeMillis() -start);

        return Response.ok(versionStatsMap.values()).build();
    }

    @POST
    @Path("/addextension")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @SecurityRequirement(name = "Authentication")
    @RolesAllowed("admin")
    @Operation(summary = "Inserts an extension in the database")
    public Response addExtension(
            @NotNull(message = "extensionId is missing") String extensionId) {
        Extension extension = watcher.addExtension(extensionId);
        if (extension == null) {
            throw new WebApplicationException(extensionId+ " already exists", Response.Status.CONFLICT);
        }
        return Response.accepted(extension.displayName +" was added").build();
    }


    private Object getStat(String col, ExtensionInstall ei) {
        return switch (col) {
            case "time" -> ei.time;
            case "delta" -> ei.delta;
            case "installs" -> ei.installs;
            case "updates" -> ei.updates;
            case "total_installed" -> ei.total_installs;
            case "onpremDownloads" -> ei.onpremDownloads;
            default -> throw new IllegalStateException("Unexpected value: " + col);
        };
    }

    private static List<String> getColumns(UriInfo uriInfo) {
        List<String> columns = uriInfo.getQueryParameters().get("stats");
        if (columns == null || columns.isEmpty()) {
            return List.of("time", "total_installed");
        }
        validate(columns);
        return columns;
    }

    private static void validate(List<String> columns) {
        StringBuilder errorMessage = new StringBuilder();
        for (String value : columns) {
            if (!VALID_STATS_PARAM_VALUES.contains(value)) {
                errorMessage.append("Invalid value for the stats parameter: \"").append(value).append("\"\n");
            }
        }
        if (errorMessage.length() > 0) {
            throw new WebApplicationException(errorMessage.append(VALID_STATS_PARAM_MSG).toString(), Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Path("{extensionId}.csv")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces("text/csv")
    public Response csv(@PathParam String extensionId) {
        Extension extension = getExtension(extensionId);
        StreamingOutput streamingOutput = (OutputStream output) -> {
            Writer writer = new BufferedWriter(new OutputStreamWriter(output));
            // Create a CSV header
            String header = "version,installs,updates,total_installed,time\n"; // Replace with your column names
            writer.write(header);

            Stream<ExtensionInstall> installs = ExtensionInstall.getFrom(extension);
            installs.map(this::toCSV).forEach(csv -> {
                try {
                    writer.write(csv);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            writer.flush();
        };
        return Response.ok(streamingOutput,
                        MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Type","'Content-Type: text/csv'")
                .header("content-disposition",
                        "attachment; filename = "+extensionId+".csv").
                build();
    }

    private static Extension getExtension(String extensionId) {
        Extension extension = Extension.findByName(extensionId);
        if (extension == null) {
            throw new NotFoundException("Unknown extension: " + extensionId);
        }
        return extension;
    }

    private String toCSV(ExtensionInstall install) {
        return install.version+","+install.installs+","+install.updates+","+install.total_installs+","+install.time+"\n";
    }


    @GET
    @Path("refresh")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance refresh() {
        watcher.refresh();
        return get();
    }

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<Extension> extensions);

        public static native TemplateInstance details(Extension extension);
    }

    @RegisterForReflection
    public class VersionStats {
        public String _id;
        public List<Map<String, ?>> events = new ArrayList<>();

        public VersionStats(String version) {
            _id=version;
        }
    }
}
