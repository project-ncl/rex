package org.jboss.pnc.rex.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class Edge {

    private final String source;

    private final String target;
}
