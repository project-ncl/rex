package org.jboss.pnc.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * Class that holds data for communicating with remote entity where a Task runs/is going to run.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
@Builder
@AllArgsConstructor(onConstructor_ = {@ProtoFactory})
public class RemoteAPI {

    @Getter(onMethod_ = {@ProtoField(number = 1)})
    private String startUrl;

    @Getter(onMethod_ = {@ProtoField(number = 2)})
    private String stopUrl;
}
