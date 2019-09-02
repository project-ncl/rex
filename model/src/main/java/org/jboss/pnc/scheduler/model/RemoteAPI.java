package org.jboss.pnc.scheduler.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.net.URL;

/**
 * Class that holds data for communicating with remote entity where a Task runs/is going to run.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
@Builder
public class RemoteAPI {

    private String startUrl;

    private String stopUrl;

    @ProtoFactory
    public RemoteAPI(String startUrl, String stopUrl) {
        this.startUrl = startUrl;
        this.stopUrl = stopUrl;
    }

    @ProtoField(number = 1)
    public String getStartUrl() {
        return startUrl;
    }

    @ProtoField(number = 2)
    public String getStopUrl() {
        return stopUrl;
    }
}
