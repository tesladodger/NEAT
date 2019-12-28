package com.tesladodger.neat;


/**
 * Contains the information of a connection in the genome.
 */
public class ConnectionGene {

    private int inNode;
    private int outNode;
    private float weight;
    private boolean expressed;
    private int innovationNumber;

    /**
     * Constructor.
     *
     * @param inNode node that sends the information;
     * @param outNode node that receives information;
     * @param weight of the connection;
     * @param expressed weather this connection is active or not;
     * @param innovationNumber see Innovation class;
     */
    public ConnectionGene (int inNode, int outNode, float weight, boolean expressed, int innovationNumber) {
        this.inNode = inNode;
        this.outNode = outNode;
        this.weight = weight;
        this.expressed = expressed;
        this.innovationNumber = innovationNumber;
    }

    public void setWeight (float weight) {
        this.weight = weight;
    }

    ConnectionGene copy () {
        return new ConnectionGene(inNode, outNode, weight, expressed, innovationNumber);
    }

    void disable () {
        expressed = false;
    }

    public int getInNode () {
        return inNode;
    }

    public int getOutNode () {
        return outNode;
    }

    float getWeight () {
        return weight;
    }

    boolean isExpressed () {
        return expressed;
    }

    public int getInnovationNumber () {
        return innovationNumber;
    }

}
