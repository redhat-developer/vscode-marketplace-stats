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

import com.redhat.devtools.stats.models.Extension;
import com.redhat.devtools.stats.models.ExtensionInstall;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.redhat.devtools.stats.utils.JsonUtils.*;

@ApplicationScoped
public class MarketPlaceStatisticsWatcher {

    @Inject
    @ConfigProperty(name = "watched.publishers")
    Set<String> watchedPublishers;


    @Inject
    @ConfigProperty(name = "read.only")
    Boolean readOnly;

    private final MarketPlaceService service;

    MarketPlaceStatisticsWatcher(MarketPlaceService service){
        this.service = service;
    }

    void onStart(@Observes StartupEvent ignoredStartup) {
        crawl();
    }

    @Scheduled(cron = "{marketplace-api.crawl.cron}")
    @Transactional
    public void crawl() {
        if (Boolean.TRUE.equals(readOnly)) {
            Log.info("Database is read-only, skipping updates");
            return;
        }
        watchedPublishers.forEach(this::updatePublisherExtensions);
        updateInstalls();
    }

    private void updatePublisherExtensions(String publisher) {
        JsonObject publisherJson = service.getPublisherData(publisher);
        // Extensions found in the marketplace
        JsonArray newExtensions = getExtensions(publisherJson);

        // Existing extensions
        List<Extension> existingExtensions = new ArrayList<>(Extension.findAll().list());
        //Add new extensions to DB
        int newOnes = newExtensions.size();
        if (newOnes > 0) {
            Log.infov("Adding/updating {0} extensions from {1}", newOnes, publisher);
            addExtensions(newExtensions, existingExtensions);
        }
    }

    private boolean changed(Extension oldOne, Extension newOne) {
        return !Objects.equals(oldOne.displayName, newOne.displayName) || !Objects.equals(oldOne.icon, newOne.icon);
    }

    private void addExtensions(JsonArray newExtensions, List<Extension> existingExtensions) {
        newExtensions.stream()
            .map(ext -> marketPlaceToExtension((JsonObject)ext))
            .forEach(ext -> createOrUpdate(ext, existingExtensions));
    }

    private void createOrUpdate(Extension extension, List<Extension> existingExtensions) {
        Optional<Extension> maybeExtension = existingExtensions.stream().filter(e -> Objects.equals(e.name, extension.name)).findFirst();
        if (maybeExtension.isEmpty()) {
            Log.infov("{0} is new, adding...", extension.name);
            extension.persist();
            return;
        }
        Extension oldOne = maybeExtension.get();
        if (changed(extension, oldOne)) {
            Log.infov("{0} changed, updating...", extension.name);
            oldOne.displayName = extension.displayName;
            oldOne.icon = extension.icon;
            oldOne.persist();
        }
        existingExtensions.remove(oldOne);
    }

    @Transactional
    public Extension addExtension(String extensionId) {
        Extension extension = Extension.findByName(extensionId);
        if (extension != null) {
            return null;
        }
        JsonObject extensionJson = service.getExtensionData(extensionId);
        if (extensionJson == null) {
            throw new NotFoundException(extensionId+ " was not found on the Marketplace");
        }
        extension = marketPlaceToExtension(extensionJson);
        extension.persistAndFlush();
        updateInstalls(extension);
        return extension;
    }

    private void updateInstalls() {
        Log.info("Updating installs");
        Extension.findActive().forEach(this::updateInstalls);
    }

    private void updateInstalls(Extension extension) {
        JsonObject marketplaceData = service.getExtensionData(extension.name);
        if (marketplaceData == null) {
            return;
        }
        Log.debugv("Updating installs for {0}",extension.name);

        JsonArray statistics = marketplaceData.getJsonArray("statistics");

        int installed = getStatAsInt(statistics, "install");
        int updated = getStatAsInt(statistics, "updateCount");
        int onpremDownloads = getStatAsInt(statistics, "onpremDownloads");
        int totalInstalled = installed + updated;
        if (onpremDownloads > -1 ) {
            totalInstalled += onpremDownloads;
        }
        int delta = 0;
        String version = marketplaceData.getJsonArray("versions").getJsonObject(0).getString("version","unknown");

        Instant now = Instant.now();

        List<ExtensionInstall> lastInstalls = ExtensionInstall.getLast2Installs(extension);
        ExtensionInstall stats = switch (lastInstalls.size()) {
            case 0 -> { // first time we're seeing this extension
                delta = totalInstalled;
                yield new ExtensionInstall();
            }
            case 1 -> { // This extension has been seen once only, so we keep that 1st record, create a new one
                delta = totalInstalled - lastInstalls.get(0).total_installs;
                yield new ExtensionInstall();
            }
            default -> { // If last seen installs is from the same day and same version, reuse it, else return a new one
                ExtensionInstall prevInstalls = lastInstalls.get(1);
                delta = totalInstalled - prevInstalls.total_installs;
                yield (version.equals(prevInstalls.version) && isSameDay(now, prevInstalls.time))? prevInstalls: new ExtensionInstall();
            }
        };


        if (stats.version == null) {
            stats.version = version;
        }
        if (stats.extension == null) {
            stats.extension = extension;
        }
        stats.installs = installed;
        stats.updates = updated;
        stats.total_installs = totalInstalled;
        stats.delta = delta;
        stats.time = now;
        if (onpremDownloads > -1 ) {
            stats.onpremDownloads = onpremDownloads;
        }
        stats.persistAndFlush();
    }

    @CacheInvalidate(cacheName = "marketplace-api")
    public void refresh() {
        crawl();
    }

    private boolean isSameDay(Instant time1, Instant time2) {
        return time1.truncatedTo(ChronoUnit.DAYS).equals(time2.truncatedTo(ChronoUnit.DAYS));
    }
}