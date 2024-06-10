/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.common.util;

import org.slf4j.MDC;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
                .collect(Collectors.toMap(
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
    public static <T> T wrapWithMDC(Map<String, String> mdcKeyMapping, Map<String, String> headers, Supplier<T> supplier) {
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
