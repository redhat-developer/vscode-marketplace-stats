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
package com.redhat.devtools.stats.security;

import java.io.IOException;
import java.security.Principal;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

/**
 * This is a very basic token authentication that check for a TOKEN that is set
 * in the Header, and if it matches the configured token, set the user as the Admin user
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 *
 * Copied from https://raw.githubusercontent.com/quarkusio/registry.quarkus.io/1597dc0761e107863629ab91f9c61bb3b013520f/src/main/java/io/quarkus/registry/app/security/SecurityOverrideFilter.java
 */
@Provider
@PreMatching
public class SecurityOverrideFilter implements ContainerRequestFilter {

    @Inject
    @ConfigProperty(name = "TOKEN")
    Instance<Optional<String>> appToken;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Optional<String> serverToken = appToken.get();
        String token = requestContext.getHeaders().getFirst("TOKEN");
        if (serverToken.isEmpty() || Objects.equals(serverToken.orElse(null), token)) {
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> "Admin";
                }

                @Override
                public boolean isUserInRole(String r) {
                    return "admin".equals(r);
                }

                @Override
                public boolean isSecure() {
                    return false;
                }

                @Override
                public String getAuthenticationScheme() {
                    return "basic";
                }
            });
        } else {
            String method = requestContext.getMethod().toUpperCase();
            if (!"GET".equalsIgnoreCase(method) &&
                    !"HEAD".equalsIgnoreCase(method)) {
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            }
        }
    }
}