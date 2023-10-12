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
package com.redhat.devtools.stats.models;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

@Entity
public class ExtensionInstall extends PanacheEntityBase {
    @Id
    @GeneratedValue(generator = "extensionInstallSequenceGenerator", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
            name = "extensionInstallSequenceGenerator",
            sequenceName = "ExtensionInstall_SEQ",
            allocationSize = 1 //No need for hibernate to be clever, insert volume is quite low
    )
    public Long id;

    @ManyToOne
    public Extension extension;
    public int delta;
    public int installs;
    public int updates;
    public String version;
    public int total_installs;
    public Instant time;
    public int onpremDownloads;

    private static final String EXTENSION_FIELD = "extension";
    private static final String TIME_FIELD = "time";

    public static ExtensionInstall getLastInstall(Extension e) {
        return find(EXTENSION_FIELD, Sort.descending(TIME_FIELD), e).firstResult();
    }

    public static ExtensionInstall getLastInstall(Extension extension, String version, Instant instant) {
        Instant date = instant.truncatedTo(ChronoUnit.DAYS);
        return find("extension=?1 and version=?2 and DATE(time)=DATE(?3) ", Sort.descending("time"), extension, version, date).firstResult();
    }


    public static List<ExtensionInstall> getLast2Installs(Extension extension) {
        return find(EXTENSION_FIELD,Sort.descending(TIME_FIELD), extension).page(0, 2).list();
    }

    public static Stream<ExtensionInstall> getFrom(Extension extension) {
        return find(EXTENSION_FIELD, Sort.ascending(TIME_FIELD), extension).stream();
    }
}
