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
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Cacheable
public class Extension extends PanacheEntityBase {

    @Id
    @GeneratedValue(generator = "extensionSequenceGenerator", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
            name = "extensionSequenceGenerator",
            sequenceName = "Extension_SEQ",
            allocationSize = 1
    )
    public Long id;

    @Column(unique = true)
    public String name;
    public String displayName;
    public boolean active = true;

    @Column(length = 500)
    public String icon;

    public static List<Extension> findActive() {
        return find("active", Sort.ascending("id"), true).list();
    }

    public static Extension findByName(String name) {
        return find("name", name).firstResult();
    }

    @RegisterForReflection
    public static record ExtensionDto (Extension extension, Integer totalInstalls){};

    public static List<ExtensionDto> findActiveSortedByPopularity() {
        String query = """
            select e, totalInstalls FROM Extension e
            LEFT JOIN LATERAL (
                SELECT ei.extension.id AS extId, max(ei.time) as latest, max(ei.total_installs) as totalInstalls
                FROM ExtensionInstall ei
                GROUP BY extId
            ) as popular ON e.id = extId
            where e.active = true
            order by totalInstalls DESC
        """;
        return find(query).project(ExtensionDto.class).list();
    }

    @Override
    public String toString() {
        return name;
    }
}
