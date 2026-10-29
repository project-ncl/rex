/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.util;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.MDC;

public class MDCUtils {

    public static void applyMDCsFromHeadersMM(Map<String, String> mdcKeyMapping, Map<String, List<String>> headers) {
        if (mdcKeyMapping == null) {
            return;
        }

        for (var entry : mdcKeyMapping.entrySet()) {
            var headerValues = headers.get(entry.getKey());
            if (headerValues != null && !headerValues.isEmpty()) {
                MDC.put(entry.getValue(), headerValues.stream().reduce((str1, str2) -> str1 + "," + str2).get());
            }
        }
    }

    public static void applyMDCsFromHeaders(Map<String, String> mdcKeyMapping, Map<String, String> headers) {
        Map<String, List<String>> mapWithList = headers.entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> List.of(entry.getValue())));

        MDCUtils.applyMDCsFromHeadersMM(mdcKeyMapping, mapWithList);
    }

    public static void wrapWithMDC(Map<String, String> mdcKeyMapping, Map<String, String> headers, Runnable runnable) {
        try {
            MDCUtils.applyMDCsFromHeaders(mdcKeyMapping, headers);

            runnable.run();
        } finally {
            MDC.clear();
        }
    }

    public static <T> T wrapWithMDC(
            Map<String, String> mdcKeyMapping,
            Map<String, String> headers,
            Supplier<T> supplier) {
        try {
            MDCUtils.applyMDCsFromHeaders(mdcKeyMapping, headers);

            return supplier.get();
        } finally {
            MDC.clear();
        }
    }

    public static <T> T wrapWithMDC(Map<String, String> mdcKeys, Supplier<T> supplier) {
        try {
            mdcKeys.forEach(MDC::put);

            return supplier.get();
        } finally {
            MDC.clear();
        }
    }
}
