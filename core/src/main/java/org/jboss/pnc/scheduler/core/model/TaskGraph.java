package org.jboss.pnc.scheduler.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Map;
import java.util.Set;

@Builder
@AllArgsConstructor
public class TaskGraph {

    @Getter
    @Singular
    private final Map<String, InitialTask> vertices;

    @Getter
    @Singular
    private final Set<Edge> edges;
}
