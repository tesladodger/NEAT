package com.tesladodger.neat;

import java.util.concurrent.RecursiveAction;


public class MultiThreadedUpdate extends RecursiveAction {

    private final Individual[] individuals;

    private final int lo, hi;

    private final int subSize;

    MultiThreadedUpdate (Individual[] individuals, int threadNumber) {
        this(individuals, 0, individuals.length, individuals.length/threadNumber);
    }

    private MultiThreadedUpdate (Individual[] individuals, int lo, int hi, int subSize) {
        this.individuals = individuals;
        this.lo = lo;
        this.hi = hi;
        this.subSize = subSize;
    }

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
