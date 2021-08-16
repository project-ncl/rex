package org.jboss.pnc.rex.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializationUtils {

    public static byte[] convertToByteArray(Object object) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(bStream)) {
            stream.writeObject(object);
            stream.flush();
        }
        return bStream.toByteArray();
    }

    public static Object convertToObject(byte[] attachment) throws IOException, ClassNotFoundException {
        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(attachment))) {
            return stream.readObject();
        }
    }
}
