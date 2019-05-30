package com.tesladodger.neat;


/**
 * Abstract class that contains the methods relating to an individual that are particular to a
 * problem. Is the contact between the user and the algorithm.
 */
public abstract class Behavior {

    /**
     * Method that returns the inputs to the neural network.
     *
     * @return n*m matrix where where m is the number of different input patterns and n is the
     *        actual number of inputs.
     */
    public abstract float[][] updateSensors () ;

    /**
     * Method where the individual acts upon the outputs of the neural network.
     *
     * @param controls raw outputs of the neural network (after sigmoid);
     */
    public abstract void move (float[][] controls) ;

    /**
     * Should return true if the individual is still alive.
     *
     * @return true if alive, false otherwise;
     */
    public abstract boolean isAlive () ;

    /**
     * Method to calculate the fitness of the individual.
     *
     * @return the fitness;
     */
    public abstract float fitnessFunction () ;

    /**
     * Should create a completely independent copy of this behavior, to be used by another
     * individual or an offspring. Only copy what's necessary, and be sure to create new
     * structures instead of returning pointers to the old ones.
     *
     * @return a copy of this behavior;
     */
    public abstract Behavior copy () ;

}
