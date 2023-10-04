/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class SerializationUtils {

    public static byte[] convertToByteArray(Object object) throws IOException {
        if (object == null) {
            return null;
        }

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(bStream)) {
            stream.writeObject(object);
            stream.flush();
        }
        return bStream.toByteArray();
    }

    public static Object convertToObject(byte[] attachment) throws IOException, ClassNotFoundException {
        if (attachment == null || attachment.length == 0) {
            return null;
        }
        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(attachment))) {
            return stream.readObject();
        }
    }
}
