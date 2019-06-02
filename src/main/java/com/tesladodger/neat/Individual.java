package com.tesladodger.neat;


/**
 * Class for the members of a population.
 * Each individual has a genome and a copy of the extended behavior class, supplied by the user.
 * For games, handles getting information for the sensors, thinking (feed forward), moving and
 * rendering.
 */
class Individual{

    private float fitness;
    private boolean alive;

    private Genome brain;

    /* Sensors and controls are n*m matrices where m is the number of different input patterns and
     * n is the actual number of inputs. */
    private float[][] sensors;
    private float[][] controls;

    private int numberSensors;
    private int numberControls;

    private Behavior behavior;


    /**
     * Constructor.
     */
    Individual (Genome brain, int numberSensors, int numberControls, Behavior behavior) {
        fitness = 0;
        alive = true;

        this.brain = brain;
        this.behavior = behavior;

        this.numberSensors = numberSensors;
        this.numberControls = numberControls;
    }


    /**
     * Makes a deep copy of this individual.
     *
     * @return new identical individual;
     */
    Individual copy () {
        Individual clone = new Individual(brain.copy(), numberSensors, numberControls, behavior.copy());
        clone.fitness = fitness;
        return clone;
    }


    /**
     * Deep copy of this individual for replay.
     *
     * @return new identical individual;
     */
    Individual copyForReplay () {
        Individual clone = new Individual(brain.copy(), numberSensors, numberControls, behavior.copyForReplay());
        clone.fitness = fitness;
        return clone;
    }


    // ------------------------------------------------------------------------  Behavior methods //
    void updateSensors () {
        sensors = behavior.updateSensors();
    }

    void think () {
        controls = new float[sensors.length][numberControls];
        int i = 0;
        for (float[] pattern : sensors) {
            controls[i++] = brain.feedForward(pattern);
        }
    }

    void move () {
        behavior.move(controls);
        alive = behavior.isAlive();
    }

    void render () {
        behavior.render();
    }

    private float fitnessFunction () {
        return behavior.fitnessFunction();
    }


    // ------------------------------------------------------------------------  Invariant methods //

    void calculateFitness () {
        fitness = fitnessFunction();
    }

    float getFitness () {
        return fitness;
    }

    void setFitness (float fitness) {
        this.fitness = fitness;
    }

    boolean isAlive () {
        return alive;
    }

    Genome getBrain () {
        return brain;
    }

    Behavior getBehavior () {
        return behavior;
    }

    int getNumberSensors () {
        return numberSensors;
    }

    int getNumberControls () {
        return numberControls;
    }
}
