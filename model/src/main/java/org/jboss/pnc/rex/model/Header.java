/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Builder
@Jacksonized
@AllArgsConstructor(onConstructor_ = {@ProtoFactory})
public class Header {

    @Getter(onMethod_ = {@ProtoField(number = 1)})
    private final String name;

    @Getter(onMethod_ = {@ProtoField(number = 2)})
    private final String value;

    @Override
    public String toString() {
        String headerValue = value;
        if (name.equals("Authorization")) {
            String method = value.split(" ", 2)[0];
            switch (method.toUpperCase()) {
                case "BASIC":
                case "DIGEST":
                case "BEARER":
                case "SCRAM":
                case "NEGOTIATE":
                case "AWS4-HMAC-SHA256":
                case "MUTUAL":
                case "HOBA":
                case "NTLM":
                    headerValue = method + " ***";
                    break;
                default:
                    headerValue = value;
            };
        }

        return '(' + name + ": " + headerValue + ')';
    }
}
