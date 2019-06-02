package com.tesladodger.neat;


/**
 * Interface that contains the methods relating to an individual that are particular to a
 * problem. Is the contact between the user and the algorithm.
 */
public interface Behavior {

    /**
     * Method that returns the inputs to the neural network.
     *
     * @return n*m matrix where where m is the number of different input patterns and n is the
     *        actual number of inputs.
     */
    float[][] updateSensors () ;

    /**
     * Method where the individual acts upon the outputs of the neural network.
     *
     * @param controls raw outputs of the neural network (after sigmoid);
     */
    void move (float[][] controls) ;

    /**
     * Used to render the Individual. Called after the move method. If 'only_show_best' option is
     * chosen, this method will be called after the simulation.
     */
    void render () ;

    /**
     * Should return true if the individual is still alive.
     *
     * @return true if alive, false otherwise;
     */
    boolean isAlive () ;

    /**
     * Method to calculate the fitness of the individual.
     *
     * @return the fitness;
     */
    float fitnessFunction () ;

    /**
     * Should create a completely independent copy of this behavior, to be used by another
     * individual or an offspring. Only copy what's necessary, and be sure to create new
     * structures instead of returning pointers to the old ones.
     *
     * @return a copy of this behavior;
     */
    Behavior copy () ;

    /**
     * When using the only_show_best option, use this to set the initial conditions of that
     * individual the same as they were in the original simulation.
     *
     * @return a copy of an individual whose initial conditions are exactly the same as the
     *          original, removing any randomness;
     */
    Behavior copyForReplay () ;
}
