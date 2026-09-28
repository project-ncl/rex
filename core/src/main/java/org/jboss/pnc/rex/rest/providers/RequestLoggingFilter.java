/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.rest.providers;

import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import java.io.IOException;

@Slf4j
public class RequestLoggingFilter {
    private static final String REQUEST_EXECUTION_START = "request-execution-start";


    @ServerRequestFilter
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(REQUEST_EXECUTION_START, System.currentTimeMillis());

        UriInfo uriInfo = requestContext.getUriInfo();
        Request request = requestContext.getRequest();

        log.info("Requested {} {}.", request.getMethod(), uriInfo.getRequestUri());

    }

    @ServerResponseFilter
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Long startTime = (Long) requestContext.getProperty(REQUEST_EXECUTION_START);

        String took;
        if (startTime == null) {
            took = "-1";
        } else {
            took = Long.toString(System.currentTimeMillis() - startTime);
        }
        log.info("Request {} completed with status {} and took {}ms",
                requestContext.getUriInfo().getPath(),
                responseContext.getStatus(),
                took);
    }
}
