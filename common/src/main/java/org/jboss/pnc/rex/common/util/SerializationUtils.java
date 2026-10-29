/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SerializationUtils {

    public static byte[] convertToByteArray(Object object) throws IOException {
        if (object == null) {
            return null;
        }
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(new GZIPOutputStream(bStream))) {
            stream.writeObject(object);
            stream.flush();
        }
        return bStream.toByteArray();
    }

    public static Object convertToObject(byte[] attachment) throws IOException, ClassNotFoundException {
        if (attachment == null || attachment.length == 0) {
            return null;
        }
        try (ObjectInputStream stream = new ObjectInputStream(
                new GZIPInputStream(new ByteArrayInputStream(attachment)))) {
            return stream.readObject();
        }
    }
}
