package com.tesladodger.neat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Contains the nodes and connections of a single network.
 * Handles mutation and crossover (static operation).
 */
class Genome {

    /* Probability of mutating the weight of all connections. */
    private static final float WEIGHT_MUTATION_PROBABILITY = 0.8f;

    /* Probability of mutating a connection to a new random number. */
    private static final float NEW_RANDOM_WEIGHT_PROBABILITY = 0.1f;

    /* Probability of disabling a connection if either parent has it disabled. */
    private static final float DISABLE_CONNECTION_PROBABILITY = 0.75f;

    /* Probability of adding a new node. */
    private static final float NEW_NODE_PROBABILITY = 0.03f;

    /* Probability of adding a new connection. */
    private static final float NEW_CONNECTION_PROBABILITY = 0.05f;

    /* Maps from innovation number to connections and nodes, and lists of all the keys
     * for random selection. */
    private Map<Integer, ConnectionGene> connections;
    private List<Integer> connectionKeys;
    private Map<Integer, NodeGene> nodes;
    private List<Integer> nodeKeys;

    /* Id of the bias node. */
    private int biasNode;

    /* How many inputs, outputs and layers in this network. */
    private int inputNumber;
    private int outputNumber;
    private int layers;


    /**
     * Constructor.
     *
     * @param inputNumber number of inputs;
     * @param outputNumber number of outputs;
     * @param fromCrossover boolean, true when this genome if being created from the copy or
     *                      crossover functions. Since the nodes will be copied, there's no
     *                      need to create the input and output nodes;
     */
    Genome (int inputNumber, int outputNumber, boolean fromCrossover) {
        if (inputNumber < 1 || outputNumber < 1) throw new
                IllegalArgumentException("Number of inputs and outputs must be natural numbers.");

        connections = new HashMap<>();
        connectionKeys = new ArrayList<>();
        nodes = new HashMap<>();
        nodeKeys = new ArrayList<>();

        this.inputNumber = inputNumber;
        this.outputNumber = outputNumber;
        layers = 2;

        if (fromCrossover) return;

        // Create the input nodes.
        for (int i = 0; i < inputNumber; i++) {
            addNodeGene(new NodeGene(NodeGene.TYPE.INPUT, nodes.size(), 0));
        }

        // Create the output nodes.
        for (int j = 0; j < outputNumber; j++) {
            addNodeGene(new NodeGene(NodeGene.TYPE.OUTPUT, nodes.size(), 1));
        }

        // Create the bias node.
        biasNode = nodes.size();
        addNodeGene(new NodeGene(NodeGene.TYPE.INPUT, nodes.size(), 0));
    }


    /**
     * Creates a deep copy of this genome.
     *
     * @return new genome;
     */
    Genome copy () {
        Genome clone = new Genome(inputNumber, outputNumber, true);

        // Add copies of the nodes to the copy genome.
        for (NodeGene nodeGene : nodes.values()) {
            clone.nodes.put(nodeGene.getId(), nodeGene.copy());
            clone.nodeKeys.add(nodeGene.getId());
        }

        // Add copies of the connections to the copy genome.
        for (ConnectionGene connectionGene : connections.values()) {
            clone.connections.put(connectionGene.getInnovationNumber(), connectionGene.copy());
            clone.connectionKeys.add(connectionGene.getInnovationNumber());
        }

        clone.biasNode = biasNode;
        clone.layers = layers;

        return clone;
    }


    /**
     * Mutates this network.
     *
     * @param r Random;
     * @param innovation innovation number counter;
     */
    void mutate (Random r, Innovation innovation) {
        // If there are no connections, connect all the inputs to the outputs.
        if (connections.size() == 0) {
            for (int i = 0; i < (inputNumber + 1) * outputNumber; i++) {
                addConnectionMutation(r, innovation);
            }
            return;
        }

        if (r.nextFloat() < WEIGHT_MUTATION_PROBABILITY) {
            weightMutation(r);
        }
        if (r.nextFloat() < NEW_CONNECTION_PROBABILITY) {
            addConnectionMutation(r, innovation);
        }
        if (r.nextFloat() < NEW_NODE_PROBABILITY) {
            addNodeMutation(r, innovation);
        }
    }


    /**
     * Mutates a connection by changing its weight.
     *
     * @param r random;
     */
    private void weightMutation (Random r) {
        for (ConnectionGene con : connections.values()) {
            if (r.nextFloat() < NEW_RANDOM_WEIGHT_PROBABILITY) {
                con.setWeight(r.nextFloat() * 2f - 1f);
            } else {
                float newWeight = con.getWeight() + (float) (r.nextGaussian() / 50f);
                con.setWeight(newWeight);
            }
        }
    }


    /**
     * Mutates the genome by adding a new connection between previously unconnected nodes.
     *
     * @param r random;
     */
    private void addConnectionMutation (Random r, Innovation innovation) {
        // No connections can be added to a fully connected network.
        if (isFullyConnected()) return;

        NodeGene node1 = nodes.get(nodeKeys.get(r.nextInt(nodeKeys.size())));
        NodeGene node2 = nodes.get(nodeKeys.get(r.nextInt(nodeKeys.size())));

        // Keep picking nodes until they are good for connecting.
        while (randomConnectionNodesAreShit(node1, node2)) {
            node1 = nodes.get(nodeKeys.get(r.nextInt(nodeKeys.size())));
            node2 = nodes.get(nodeKeys.get(r.nextInt(nodeKeys.size())));
        }

        // Check if the connection is reversed and swap the nodes.
        if (node1.getLayer() > node2.getLayer()) {
            NodeGene temp = node1;
            node1 = node2;
            node2 = temp;
        }

        // Random weight for the new connection.
        float weight = r.nextFloat()*2f - 1f;

        // Get the innovation number.
        int number = innovation.getInnovationNumber(node1.getId(), node2.getId());

        // Create the connection.
        ConnectionGene newConnection = new ConnectionGene(node1.getId(), node2.getId(),
                weight, true, number);
        connections.put(newConnection.getInnovationNumber(), newConnection);
        connectionKeys.add(newConnection.getInnovationNumber());
    }


    /**
     * Checks if two nodes cannot be connected.
     *
     * @param n1 node;
     * @param n2 node;
     *
     * @return boolean true if the nodes cannot be connected;
     */
    private boolean randomConnectionNodesAreShit (NodeGene n1, NodeGene n2) {
        // Check if the connection exists.
        boolean connectionExists = false;
        for (ConnectionGene con : connections.values()) {
            if (con.getInNode() == n1.getId() && con.getOutNode() == n2.getId()) {
                connectionExists = true;
                break;
            }
            else if (con.getInNode() == n2.getId() && con.getOutNode() == n1.getId()) {
                connectionExists = true;
                break;
            }
        }
        if (connectionExists) return true;

        // Check if the nodes are in the same layer.
        return (n1.getLayer() == n2.getLayer()) ;
    }


    /**
     * Mutate the genome by adding a new node in the middle of a random connection.
     *
     * @param r random;
     */
    private void addNodeMutation (Random r, Innovation innovation) {

        // Pick a random connection.
        ConnectionGene con = connections.get(connectionKeys.get(r.nextInt(connectionKeys.size())));

        // Try not to separate the bias node.
        while (con.getInNode() == biasNode && connections.size() != 1) {
            con = connections.get(connectionKeys.get(r.nextInt(connectionKeys.size())));
        }

        // Get the nodes from that connection.
        NodeGene inNode = nodes.get(con.getInNode());
        NodeGene outNode = nodes.get(con.getOutNode());

        // Disable the connection.
        con.disable();

        // Create a new hidden node. The layer is the inNode's layer + 1.
        NodeGene newNode = new NodeGene(NodeGene.TYPE.HIDDEN, nodes.size(), inNode.getLayer()+1);

        // If the layer of the new node is equal to the layer of the outNode, a new layer needs to
        // be created and all the nodes on the layer greater or equal to the output need to change
        // layer.
        if (newNode.getLayer() == outNode.getLayer()) {
            for (NodeGene n : nodes.values()) {
                if (n.getLayer() >= newNode.getLayer()) {
                    n.incrementLayer();
                }
            }
            layers++;
        }

        // The connection to the new node has a weight of 1 and the connection from it has the
        // weight of the previous connection.
        int number = innovation.getInnovationNumber(inNode.getId(), newNode.getId());
        ConnectionGene toNew = new ConnectionGene(inNode.getId(), newNode.getId(),
                1f, true, number);
        number = innovation.getInnovationNumber(newNode.getId(), outNode.getId());
        ConnectionGene fromNew = new ConnectionGene(newNode.getId(), outNode.getId(),
                con.getWeight(), true, number);

        // Put the new node and new connections in the maps.
        addNodeGene(newNode);
        addConnectionGene(toNew);
        addConnectionGene(fromNew);
    }


    /**
     * Returns whether this network is fully connected.
     *
     * @return true if fully connected;
     */
    private boolean isFullyConnected () {
        // Each index contains the number of nodes the layer.
        int[] nodesInLayers = new int[layers];

        for (NodeGene n : nodes.values()) {
            nodesInLayers[n.getLayer()] += 1;
        }

        // The total number of connections is the sum of the product of the number of nodes in the
        // layers and the number of nodes in subsequent layers.
        int totalConnections = 0;
        for (int i = 0; i < layers-1; i++) {
            int nodesInFront = 0;
            for (int j = i+1; j < layers; j++) {
                nodesInFront += nodesInLayers[j];
            }
            totalConnections += nodesInLayers[i] * nodesInFront;
        }

        // If the number of connections is already the total number possible, return true.
        return connections.size() == totalConnections;
    }


    /**
     * Feeds the inputs to the neural network.
     *
     * @param inputs from the individual's sensors;
     *
     * @return the output of the neural network;
     */
    float[] feedForward (float[] inputs) {
        if (inputs.length != inputNumber) throw new IllegalArgumentException("" +
                "The input array must match the number of inputs of the network.");

        // Order the nodes in an array by layer.
        NodeGene[] orderedNodes = new NodeGene[nodes.size()];
        int index = 0;
        for (int l = 0; l < layers; l++) {
            for (NodeGene n : nodes.values()) {
                if (n.getLayer() == l) {
                    // Reset the values of the node.
                    n.reset();
                    // Add it to the array.
                    orderedNodes[index++] = n;
                }
            }
        }

        // Set the values of the input nodes.
        for (int i = 0; i < inputNumber; i++) {
            // Use nodeKeys because they are ordered like the inputs.
            nodes.get(nodeKeys.get(i)).addToInput(inputs[i]);
        }

        // Set the value of the bias node.
        nodes.get(biasNode).addToInput(1);

        // Create the list of connections for every node and engage it.
        for (NodeGene n : orderedNodes) {
            List<ConnectionGene> connectionsFromCurrentNode = new ArrayList<>();
            for (ConnectionGene con : connections.values()) {
                if (nodes.get(con.getInNode()) == n) {
                    connectionsFromCurrentNode.add(con);
                }
            }
            if (n.getType() != NodeGene.TYPE.OUTPUT) {
                n.engage(connectionsFromCurrentNode, nodes);
            }
        }

        // Get the values of the output nodes.
        float[] output = new float[outputNumber];
        index = 0;
        for (int i = inputNumber; i < inputNumber + outputNumber; i++) {
            // Again, use the keys.
            output[index++] = nodes.get(nodeKeys.get(i)).getOutput();
        }

        return output;
    }


    /**
     * Performs crossover between two genomes.
     *
     * @param parent1 more fit parent;
     * @param parent2 other parent;
     * @param r Random;
     *
     * @return new genome;
     */
    static Genome crossover (Genome parent1, Genome parent2, Random r) {
        Genome child = new Genome(parent1.inputNumber, parent1.outputNumber, true);

        // Take all the nodes from the fittest parent.
        for (NodeGene p1Node : parent1.getNodes().values()) {
            child.addNodeGene(p1Node.copy());
        }

        child.layers = parent1.layers;
        child.biasNode = parent1.biasNode;

        // Add the connections to the child.
        for (ConnectionGene p1Con : parent1.connections.values()) {
            if (parent2.getConnections().containsKey(p1Con.getInnovationNumber())) { // Matching gene
                // Create the connection.
                ConnectionGene childConGene = new ConnectionGene(
                        p1Con.getInNode(), p1Con.getOutNode(),
                        p1Con.getWeight(), p1Con.isExpressed(), p1Con.getInnovationNumber());

                // Disable the gene if either parent has it disabled.
                if (!p1Con.isExpressed() || !parent2.getConnections().get(p1Con.getInnovationNumber()).isExpressed()) {
                    if (r.nextFloat() < DISABLE_CONNECTION_PROBABILITY) {
                        childConGene.disable();
                    }
                }

               child.addConnectionGene(childConGene);
            }
            else { // Disjoint gene
                // Add all disjoint genes from the fittest parent.
                ConnectionGene childConGene = p1Con.copy();
                child.addConnectionGene(childConGene);
            }
        }

        return child;
    }


    /* ------------------------------------------------------------------------  Utility functions */

    private void addNodeGene (NodeGene node) {
        nodes.put(node.getId(), node);
        nodeKeys.add(node.getId());
    }

    private void addConnectionGene (ConnectionGene connection) {
        connections.put(connection.getInnovationNumber(), connection);
        connectionKeys.add(connection.getInnovationNumber());
    }

    private Map<Integer, NodeGene> getNodes () {
        return nodes;
    }

    Map<Integer, ConnectionGene> getConnections () {
        return connections;
    }

    List<Integer> getConnectionKeys () {
        return connectionKeys;
    }


    /**
     * Prints a genome in a similar style of the original NEAT paper.
     *
     * @param genome to print;
     */
    static void printGenome (Genome genome) {
        System.out.print("\n" + genome);

        // Print the nodes in layers.
        for (int i = genome.layers-1; i >= 0; i--) {
            System.out.print("\n Layer " + i + " : ");
            for (NodeGene n : genome.nodes.values()) {
                if (n.getLayer() == i) {
                    System.out.printf("  %2s %-6s  ", n.getId(), n.getType());
                }
            }
        }

        // Print the connections.
        System.out.print("\nConnections  :  \n");

        for (Integer conKey : genome.connectionKeys) {
            System.out.printf("|  %3s   ", genome.connections.get(conKey).getInnovationNumber());
        }
        System.out.println("|");
        for (Integer conKey : genome.connectionKeys) {
            System.out.printf("| %2s->%2s ",
                    genome.connections.get(conKey).getInNode(),
                    genome.connections.get(conKey).getOutNode());
        }
        System.out.println("|");
        for (Integer conKey : genome.connectionKeys) {
            System.out.printf("| %5s  ", genome.connections.get(conKey).isExpressed() ? " " : "DISAB");
        }
        System.out.println("|");
        for (Integer conKey : genome.connectionKeys) {
            System.out.printf("|%8.5f", genome.connections.get(conKey).getWeight());
        }
        System.out.println("|");
        System.out.println();
    }

}
