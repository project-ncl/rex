package org.jboss.pnc.rex.core.common;

import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.jboss.pnc.rex.core.common.TestData.getRequestWithStart;
import static org.jboss.pnc.rex.core.common.TestData.getStopRequest;

public class RandomDAGGeneration {

    private static final Logger logger = LoggerFactory.getLogger(RandomDAGGeneration.class);

    /**
     *
     *
     * @see <a href="https://stackoverflow.com/a/57321815">Link to algorithm rewritten from python to java</a>
     * @param seed seed used in pseudo-random generator
     * @param minPerRank minimum amount of nodes at a particular height of DAG
     * @param maxPerRank maximum amount of nodes at a particular height of DAG
     * @param minRanks minimum height of DAG
     * @param maxRanks maximum height of DAG
     * @param edgeProbability probability of an edge between node of lower rank and a node of a higher rank
     * @return randomly generated DAG
     */
    public static CreateGraphRequest generateDAG(int seed, int minPerRank, int maxPerRank, int minRanks, int maxRanks, float edgeProbability) {
        Random random = new Random(seed);
        int nodes = 0;
        int nodeCounter = 0;
        
        int ranks = random.nextInt(maxRanks) + minRanks;

        List<EdgeDTO> egdes = new ArrayList<>();
        List<List<Integer>> rankList = new ArrayList<>();
        for (int i = 0; i < ranks; i++) {
            int newNodes = random.nextInt(maxPerRank) + minPerRank;
            
            List<Integer> list = new ArrayList<>();
            for (int j = 0; j < newNodes; j++) {
                list.add(nodeCounter);
                nodeCounter++;
            }
            rankList.add(list);
            
            if (i > 0) {
                for (Integer j : rankList.get(i - 1)) {
                    for (int k = 0; k < newNodes; k++) {
                        if (random.nextFloat() < edgeProbability) {
                            egdes.add(new EdgeDTO(String.valueOf(j),String.valueOf(k+nodes)));
                        }
                    }
                }
            }
            nodes += newNodes;
        }

        CreateGraphRequest.CreateGraphRequestBuilder builder = CreateGraphRequest.builder();
        for (int i = 0; i < nodes; i++) {
            String string = String.valueOf(i);
            builder.vertex(string, CreateTaskDTO.builder()
                    .name(string)
                    .controllerMode(Mode.ACTIVE)
                    .remoteStart(getRequestWithStart(string))
                    .remoteCancel(getStopRequest(string))
                    .build());
        }
        builder.edges(egdes);
        return builder.build();
    }

    public static CreateGraphRequest generateDAG(int minPerRank, int maxPerRank, int minRanks, int maxRanks, float edgeProbability) {
        int randomSeed = new Random().nextInt();
        logger.info("Using a seed " + randomSeed + " to generate random DAG for testing");

        return generateDAG(randomSeed, minPerRank, maxPerRank, minRanks, maxRanks, edgeProbability);
    }

    /**
     * Generated output that can be pasted into http://dagitty.net/dags.html
     *
     * @param args
     */
    public static void main(String[] args) {
        for (int i = 0; i < 1; i++) {
            CreateGraphRequest request = generateDAG(2, 10, 5, 10, 0.7F);
            System.out.println(request.toString());
            for (String s : request.getVertices().keySet()) {
                System.out.println(s);
            }
            System.out.println();
            for (EdgeDTO edge : request.getEdges()) {
                System.out.println(edge.getSource() + ' ' + edge.getTarget());
            }
        }
    }
}
