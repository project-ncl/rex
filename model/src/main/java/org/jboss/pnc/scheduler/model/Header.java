package org.jboss.pnc.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Builder
@AllArgsConstructor(onConstructor_ = {@ProtoFactory})
public class Header {

    @Getter(onMethod_ = {@ProtoField(number = 1)})
    private final String name;

    @Getter(onMethod_ = {@ProtoField(number = 2)})
    private final String value;
}
