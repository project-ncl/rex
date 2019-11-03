package org.jboss.pnc.scheduler.core.model;

import lombok.Builder;
import lombok.Data;
import org.infinispan.protostream.annotations.ProtoField;

import java.net.URL;

/**
 * Class that holds data for communicating with remote entity where a Service runs/is going to run.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
@Data
@Builder
public class RemoteAPI {
    private String startUrl;
    private String stopUrl;
    //private URL pingUrl;
}
