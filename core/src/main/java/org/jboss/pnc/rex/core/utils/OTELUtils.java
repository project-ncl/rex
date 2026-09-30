/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.utils;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.annotation.Nullable;
import org.jboss.pnc.api.constants.MDCHeaderKeys;

import java.util.HashMap;
import java.util.Map;

public class OTELUtils {
    public static Map<String, String> getOTELContext() {
        Map<String, String> toReturn = new HashMap<>();

        Span span = Span.current();
        W3CTraceContextPropagator traceParentGen = W3CTraceContextPropagator.getInstance();
        if (span != null) {
            SpanContext context = span.getSpanContext();
            toReturn.put(MDCHeaderKeys.SPAN_ID.getMdcKey(), context.getSpanId());
            toReturn.put(MDCHeaderKeys.TRACE_ID.getMdcKey(), context.getTraceId());
            toReturn.put(MDCHeaderKeys.TRACE_FLAGS.getMdcKey(), context.getTraceFlags().asHex());
            // sets TRACE_PARENT and TRACE_STATE(encoded)
            traceParentGen.inject(Context.current(), toReturn, (map, key, value) -> map.put(key, value));
        }
        return toReturn;
    }

    public static Context setOTELContext(Map<String, String> otelContext) {
        W3CTraceContextPropagator traceParentGen = W3CTraceContextPropagator.getInstance();

        return traceParentGen.extract(Context.current(), otelContext, MapMapGetter.INSTANCE);
    }

    public static class MapMapGetter implements TextMapGetter<Map<String, String>> {

        public static MapMapGetter INSTANCE = new MapMapGetter();

        private MapMapGetter() {}

        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Nullable
        @Override
        public String get(@Nullable Map<String, String> carrier, String key) {
            return carrier == null ? null : carrier.get(key);
        }
    }



}
