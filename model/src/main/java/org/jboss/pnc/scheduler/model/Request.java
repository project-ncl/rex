package org.jboss.pnc.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;
import org.jboss.pnc.scheduler.common.enums.Method;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@Jacksonized
@Slf4j
public class Request {

    @Getter(onMethod_ = {@ProtoField(number = 1)})
    private final String url;

    @Getter(onMethod_ = {@ProtoField(number = 2)})
    private final Method method;

    @Getter(onMethod_ = {@ProtoField(number = 3)})
    private final List<Header> headers;

    @Getter
    private final Object attachment;

    @ProtoFactory
    public Request(String url, Method method, List<Header> headers, byte[] byteAttachment) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.attachment = convertToObject(byteAttachment);
    }

    @ProtoField(number = 4, type = Type.BYTES)
    public byte[] getByteAttachment() {
        return convertToByteArray(attachment);
    }

    private byte[] convertToByteArray(Object object) {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(bStream)) {
            stream.writeObject(object);
            stream.flush();
        } catch (IOException exception) {
            log.error("Unexpected IO error when serializing Request.class attachment. " + this, exception);
        }
        return bStream.toByteArray();
    }

    private Object convertToObject(byte[] attachment) {
        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(attachment))) {
            return stream.readObject();
        } catch (IOException exception) {
            log.error("Unexpected IO error during construction of Request.class object. " + this, exception);
        } catch (ClassNotFoundException exception) {
            log.error("Attachment byte array could not be casted into existing class. " + this, exception);
        }
        return null;
    }
}
