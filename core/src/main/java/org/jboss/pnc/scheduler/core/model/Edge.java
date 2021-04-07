package org.jboss.pnc.scheduler.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Edge {

    private final String source;

    private final String target;
}
