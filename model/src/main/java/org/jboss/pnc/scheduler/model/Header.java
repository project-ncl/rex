package org.jboss.pnc.scheduler.model;

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
        return '(' + name + ": " + value + ')';
    }
}
