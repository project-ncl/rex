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

    //TODO remove once you are sure you don't need it
    /*private final Map<String, Set<String>> linkedEdges;

    private final Set<String> verticesSet;

    public List<String> getTopologicallyOrderedVertices() {
        List<String> resultList = new ArrayList<>();
        Set<String> tempMarked = new HashSet<>();
        Set<String> permMarked = new HashSet<>();

        for (String vertex : verticesSet) {
            visit(vertex, resultList, tempMarked, permMarked);
        }
        return resultList;
    }

    private void visit(String node, List<String> resultList, Set<String> tempMarked, Set<String> permMarked) {
        if (permMarked.contains(node)) {
            return;
        }
        if (tempMarked.contains(node)) {
            throw new RuntimeException("CYCLE");
        }
        tempMarked.add(node);
        if (linkedEdges.containsKey(node)) {
            for (String child : linkedEdges.get(node)) {
                visit(child, resultList, tempMarked, permMarked);
            }
        }
        tempMarked.remove(node);
        permMarked.add(node);
        resultList.add(node);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, InitialTask> vertices = new HashMap<>();
        private final Set<Edge> edges = new HashSet<>();
        private final Map<String, Set<String>> linkedEdges = new HashMap<>();
        private final Set<String> verticesSet = new HashSet<>();

        public Builder addVertex(InitialTask vertex) {
            this.vertices.put(vertex.getName(), vertex);
            return this;
        }

        public Builder addVertex(String name, InitialTask vertex) {
            this.vertices.put(name, vertex);
            return this;
        }

        public Builder addEdge(Edge edge) {
            edges.add(edge);
            verticesSet.add(edge.getSource());
            verticesSet.add(edge.getTarget());
            if (linkedEdges.containsKey(edge.getSource())) {
                linkedEdges.get(edge.getSource()).add(edge.getTarget());
            } else {
                Set<String> newSet = new HashSet<>();
                newSet.add(edge.getTarget());
                linkedEdges.put(edge.getSource(), newSet);
            }
            return this;
        }

        public Builder addEdge(String source, String target) {
            return addEdge(new Edge(source, target));
        }

        public TaskGraph build() {
            return new TaskGraph(vertices, edges, linkedEdges, verticesSet);
        }
    }*/

}
