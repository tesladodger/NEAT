package com.tesladodger.neat;

import java.util.List;
import java.util.Map;


/**
 * Contains the information of a node in the genome.
 */
class NodeGene {

    public enum TYPE {
        INPUT,
        HIDDEN,
        OUTPUT,
        ;
    }

    private TYPE type;
    private int id;
    private int layer;

    private float input;

    /**
     * Constructor.
     *
     * @param type input, hidden or output node;
     * @param id of the next node in the genome it's created;
     * @param layer that the node occupies in the structure;
     */
    NodeGene (TYPE type, int id, int layer) {
        this.type = type;
        this.id = id;
        this.layer = layer;

        input = 0;
    }

    /**
     * Adds to the input value of the nodes this node is connected to.
     *
     * @param connections list of connections from this node;
     *
     * @param nodes all the nodes in the network;
     */
    void engage (List<ConnectionGene> connections, Map<Integer, NodeGene> nodes) {
        for (ConnectionGene con : connections) {
            // Ignore unexpressed connections.
            if (con.isExpressed()) {
                float output = sigmoidTF(input) * con.getWeight();
                nodes.get(con.getOutNode()).addToInput(output);
            }
        }
    }

    /**
     * Calculates the sigmoidal transfer function.
     *
     * @param x value;
     *
     * @return sigmoid;
     */
    private float sigmoidTF (float x) {
        return 1f / (1f + (float) Math.pow(Math.E, -4.9f * x));
    }

    /**
     * Other nodes that are connected to this node increment the input value;
     *
     * @param value output from another node;
     */
    void addToInput (float value) {
        input += value;
    }

    float getOutput () {
        return sigmoidTF(input);
    }

    void reset () {
        input = 0;
    }

    NodeGene copy () {
        return new NodeGene(type, id, layer);
    }

    TYPE getType () {
        return type;
    }

    int getId () {
        return id;
    }

    int getLayer () {
        return layer;
    }

    void incrementLayer () {
        layer += 1;
    }
}
