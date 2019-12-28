import com.tesladodger.neat.ConnectionGene;
import com.tesladodger.neat.Genome;
import com.tesladodger.neat.Innovation;
import com.tesladodger.neat.NodeGene;

import java.util.List;
import java.util.Map;
import java.util.Random;

// todo when creating a node mutation, check if the number of the new node makes sense.

public class GenomeTest {

    private static void unitTestInnovationTracking () {
        Random r = new Random();
        Innovation innovation = new Innovation();

        Genome genome0 = new Genome(50, 50, false);
        Genome genome1 = new Genome(50, 50, false);
        Genome genome2 = new Genome(50, 50, false);

        genome0.mutate(r, innovation);
        genome1.mutate(r, innovation);
        genome2.mutate(r, innovation);

        Map<Integer, ConnectionGene> connectionGeneMap0 = genome0.getConnections();
        Map<Integer, ConnectionGene> connectionGeneMap1 = genome1.getConnections();
        Map<Integer, ConnectionGene> connectionGeneMap2 = genome2.getConnections();
        List<Integer> connectionGeneKeys0 = genome0.getConnectionKeys();
        for (Integer connKey : connectionGeneKeys0) {
            assert connectionGeneMap0.get(connKey).getInNode() == connectionGeneMap1.get(connKey).getInNode();
            assert connectionGeneMap0.get(connKey).getOutNode() == connectionGeneMap1.get(connKey).getOutNode();
            assert connectionGeneMap0.get(connKey).getInNode() == connectionGeneMap2.get(connKey).getInNode();
            assert connectionGeneMap0.get(connKey).getOutNode() == connectionGeneMap2.get(connKey).getOutNode();
        }

        for (int i = 0; i < 1000; i++) {
            genome0.addNodeMutation(r, innovation);
            genome1.addNodeMutation(r, innovation);
            genome2.addNodeMutation(r, innovation);
        }

        for (Integer connKey : genome0.getConnectionKeys()) {
            if (genome1.getConnectionKeys().contains(connKey)) {
                assert genome0.getConnections().get(connKey).getInNode() == genome1.getConnections().get(connKey).getInNode();
                assert genome0.getConnections().get(connKey).getOutNode() == genome1.getConnections().get(connKey).getOutNode();
            }
            if (genome2.getConnectionKeys().contains(connKey)) {
                assert genome0.getConnections().get(connKey).getInNode() == genome2.getConnections().get(connKey).getInNode();
                assert genome0.getConnections().get(connKey).getOutNode() == genome2.getConnections().get(connKey).getOutNode();
            }
        }

    }

    private static void unitTestIsFullyConnected () {
        Random r = new Random();
        Innovation innovation = new Innovation();

        Genome genome0 = new Genome(1, 1, false);
        assert !genome0.isFullyConnected();
        genome0.addConnectionMutation(r, innovation);
        assert !genome0.isFullyConnected();
        genome0.addConnectionMutation(r, innovation);
        assert genome0.isFullyConnected();
    }

    private static void unitTestFeedForward () {
        Random r = new Random();
        Innovation innovation = new Innovation();

        Genome genome = new Genome(2, 1, false);
        genome.addConnectionGene(new ConnectionGene(0, 2, 20, true, 4));
        genome.addNodeMutation(r, innovation);

        genome.getConnections().get(1).setWeight(-20);
        genome.addNodeGene(new NodeGene(NodeGene.TYPE.HIDDEN, 5, 1));
        genome.addConnectionGene(new ConnectionGene(0, 5, 20, true, 5));
        genome.addConnectionGene(new ConnectionGene(1, 4, 20, true, 6));
        genome.addConnectionGene(new ConnectionGene(1, 5, 20, true, 7));
        genome.addConnectionGene(new ConnectionGene(5, 2, 20, true, 8));

        Genome.printlnGenome(genome);
        System.out.println("\n\n\n");

    }

    private static void unitTests () {
        unitTestInnovationTracking();
        unitTestIsFullyConnected();
        //unitTestFeedForward();
    }

    public static void main (String[] args) {

        Genome.NEW_NODE_PROBABILITY = 0;
        Genome.WEIGHT_MUTATION_PROBABILITY = 0;
        Genome.NEW_CONNECTION_PROBABILITY = 0;

        Innovation innovation = new Innovation();
        Random r = new Random();

        unitTests();


        System.out.println("--  Test cloning  --\n");

        Genome Dolly = new Genome(2, 1, true);
        Genome.printlnGenome(Dolly);

        Dolly = new Genome(2, 1, false);
        Genome.printlnGenome(Dolly);

        Genome DollyClone = Dolly.copy();
        Genome.printlnGenome(DollyClone);


        System.out.println("\n\n-- Test Mutation and Innovation tracking  --\n");

        Dolly.mutate(r, innovation);
        DollyClone.mutate(r, innovation);

        Genome.printlnGenome(Dolly);
        Genome.printlnGenome(DollyClone);

        Genome genome0 = new Genome(4, 2, false);
        Genome genome1 = new Genome(4, 2, false);
        genome0.mutate(r, innovation);
        genome1.mutate(r, innovation);

        for (int i = 0; i < 5; i++) {
            genome0.addNodeMutation(r, innovation);
            genome1.addNodeMutation(r, innovation);
        }

        Genome.printlnGenome(genome0);
        Genome.printlnGenome(genome1);


        System.out.println("\n\n -- Test Feed Forward  --");

    }
}
