package com.tesladodger.neat;

import java.util.concurrent.RecursiveAction;


/**
 * Given the specified number of threads, the population array is updated in parallel using a
 * resultless Fork-Join task.
 */
public class MultiThreadedUpdate extends RecursiveAction {

    private final Individual[] individuals;

    /* Low and High indexes this thread is working with. */
    private final int lo, hi;

    /* Number of individuals / number of threads, as specified. */
    private final int subSize;

    /**
     * Initial Constructor.
     *
     * @param individuals array;
     * @param threadNumber # of threads;
     */
    MultiThreadedUpdate (Individual[] individuals, int threadNumber) {
        this(individuals, 0, individuals.length, individuals.length/threadNumber);
    }

    /**
     * Recursive Constructor.
     *
     * @param individuals array;
     * @param lo index;
     * @param hi index;
     * @param subSize # individuals / thread;
     */
    private MultiThreadedUpdate (Individual[] individuals, int lo, int hi, int subSize) {
        this.individuals = individuals;
        this.lo = lo;
        this.hi = hi;
        this.subSize = subSize;
    }

    /**
     * Implementation of the abstract method.
     * When the sub array is small enough, the singleThreadUpdate method is called.
     */
    protected void compute () {
        if (hi - lo <= subSize) {
            singleThreadUpdate(lo, hi);
        }
        else {
            int mid = (lo + hi) >>> 1;
            invokeAll(
                    new MultiThreadedUpdate(individuals, lo, mid, subSize),
                    new MultiThreadedUpdate(individuals, mid, hi, subSize));
        }
    }

    /**
     * Loops the sub array, updating the individuals.
     *
     * @param lo index;
     * @param hi index;
     */
    private void singleThreadUpdate (int lo, int hi) {
        for (int i = lo; i < hi; i++) {
            if (individuals[i].isAlive()) {
                individuals[i].updateSensors();
                individuals[i].think();
                individuals[i].move();
                individuals[i].render();
            }
        }
    }

}
