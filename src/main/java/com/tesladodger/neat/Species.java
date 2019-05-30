package com.tesladodger.neat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * A species is a collection of individuals whose genomes have similar topology.
 */
class Species {

    /* Threshold to accept a member to this species. */
    private static final float COMPATIBILITY_THRESHOLD = 3.0f;

    /* Coefficients to assert similarity between the rep and another genome. */
    private static final float COMPAT_COEF_1 = 1.0f;
    private static final float COMPAT_COEF_3 = 0.4f;

    /* Probability of a child being created without crossover. */
    private static final float MUTATION_WITHOUT_CROSSOVER_PROBABILITY = 0.25f;


    /* List of members of this species. */
    private List<Individual> members;

    /* The representative genome for this species, which is the best from the previous generation. */
    private Genome rep;

    /* Individual with the highest fitness ever for this species. */
    private Individual best;

    /* How many generation without improvement a species is allowed to live. */
    private static final int STALENESS_THRESHOLD = 15;

    /* How many generations without improvement. */
    private int staleness;

    /* Sum of the adjusted fitnesses of a generation. */
    private float adjustedFitnessSum;


    /**
     * Constructor.
     *
     * @param firstMember to be added;
     */
    Species (Individual firstMember) {
        rep = firstMember.getBrain().copy();
        best = firstMember.copy();

        staleness = -1;

        members = new ArrayList<>();
        members.add(firstMember);
    }


    /**
     * Method to assert whether a genome belongs to this species.
     *
     * @param candidate to specify;
     *
     * @return true if the candidate belongs to this species;
     */
    boolean canAccept (Genome candidate) {

        float excess_disjoint = (float) calculateExcessDisjointNumber(candidate);
        float average_weight_diff = calculateAverageWeightDiff(candidate);

        int normaliser = candidate.getConnectionKeys().size() - 20;
        if (normaliser < 1) {
            normaliser = 1;
        }

        float compatibility = (COMPAT_COEF_1 * excess_disjoint) / normaliser;
        compatibility += COMPAT_COEF_3 * average_weight_diff;

        return compatibility <= COMPATIBILITY_THRESHOLD;
    }


    /**
     * Calculates the number of excess and disjoint genes between the rep and a candidate.
     *
     * @param candidate being evaluated;
     *
     * @return sum of excess and disjoint genes;
     */
    private int calculateExcessDisjointNumber (Genome candidate) {
        // Loop through the keys of the rep and the candidate. If they match, increment the
        // matching number.
        int matching = 0;
        for (Integer gene1 : rep.getConnectionKeys()) {
            for (Integer gene2 : candidate.getConnectionKeys()) {
                if (gene1.equals(gene2)) {
                    matching++;
                }
            }
        }
        return rep.getConnectionKeys().size() + candidate.getConnectionKeys().size() - 2 * matching;
    }


    /**
     * Calculates the average weight difference between the rep and a candidate.
     *
     * @param candidate being evaluated;
     *
     * @return average weight difference;
     */
    private float calculateAverageWeightDiff (Genome candidate) {
        if (rep.getConnectionKeys().size() == 0 || candidate.getConnectionKeys().size() == 0) {
            return 0f;
        }

        // Loop through all the connections. If any match, add their difference to the sum and
        // increment the matching number. After that, divide the sum by the number of matching
        // connections.
        float matching = 0;
        float sum = 0;
        for (ConnectionGene con1 : rep.getConnections().values()) {
            for (ConnectionGene con2 : candidate.getConnections().values()) {
                if (con1.getInnovationNumber() == con2.getInnovationNumber()) {
                    matching++;
                    sum += Math.abs(con1.getWeight() - con2.getWeight());
                    break;
                }
            }
        }
        // Don't divide by 0;
        if (matching == 0) {
            return 100f;
        }
        return sum / matching;
    }


    /**
     * Adds a new Individual to this species.
     *
     * @param newMember to add;
     */
    void addToSpecies (Individual newMember) {
        members.add(newMember);
    }


    /**
     * Creates a child for the next generation.
     *
     * @return new individual;
     */
    Individual makeAChild (Random r, Innovation innovation) {
        Individual child;

        if (r.nextFloat() < MUTATION_WITHOUT_CROSSOVER_PROBABILITY) {
            child = pickAMember(r).copy();
        }

        else {
            Individual parent1 = pickAMember(r);
            Individual parent2 = pickAMember(r);

            child = new Individual((parent1.getFitness() > parent2.getFitness()) ?
                    Genome.crossover(parent1.getBrain(), parent2.getBrain(), r) :
                    Genome.crossover(parent2.getBrain(), parent1.getBrain(), r),
                    parent1.getNumberSensors(),
                    parent1.getNumberControls(),
                    parent1.getBehavior().copy()
            );
        }

        child.getBrain().mutate(r, innovation);
        return child;
    }


    /**
     * Selects a random member based on the fitnesses.
     *
     * @param r Random;
     *
     * @return a member;
     */
    private Individual pickAMember (Random r) {
        float fitnessSum = 0;
        for (Individual ind : members) {
            fitnessSum += ind.getFitness();
        }

        float rand = r.nextFloat()*fitnessSum;
        float runningSum = 0;

        for (Individual ind : members) {
            runningSum += ind.getFitness();
            if (runningSum > rand) {
                return ind;
            }
        }

        return members.get(0);
    }


    /**
     * Call the fitness function of every member of this species.
     */
    void calculateIndividualFitnesses () {
        for (Individual ind : members) {
            ind.calculateFitness();
        }
    }


    /**
     * Sorts the members of this species by their fitness number (descending order).
     * (Insertion sort is awesome, btw).
     * After, it updates the best individual of this species (if it improved) and its genome is the
     * new rep.
     */
    void sort () {
        int i = 1;
        while (i < members.size()) {
            Individual ind = members.get(i);
            int j = i - 1;
            while (j >= 0 && members.get(j).getFitness() < ind.getFitness()) {
                members.set(j+1, members.get(j));
                j--;
            }
            members.set(j+1, ind);
            i++;
        }

        if (members.size() == 0) {
            staleness = 20;
            return;
        }

        rep = members.get(0).getBrain().copy();
        if (members.get(0).getFitness() > best.getFitness()) {
            best = members.get(0).copy();
            staleness = 0;
        }
        else {
            staleness++;
        }
    }


    /**
     * Kill the worse half of the species.
     */
    void thanos () {
        int size = members.size();
        if (size > 2) {
            members.subList(size/2, size).clear();
        }
    }


    /**
     * Normalize the fitness of the members of this species by dividing by the number of elements.
     */
    void normalizeFitness () {
        for (Individual i : members) {
            i.setFitness(i.getFitness() / members.size());
        }
    }


    /**
     * Calculates the average fitness (after normalization) of the current generation.
     */
    void calculateAdjustedFitnessSum () {
        float sum = 0;
        for (Individual i : members) {
            sum += i.getFitness();
        }
        adjustedFitnessSum = sum;
    }


    /**
     * Returns the sum of adjusted fitnesses (after normalization) of the current generation.
     *
     * @return average fitness;
     */
    float getAdjustedFitnessSum() {
        return adjustedFitnessSum;
    }


    /**
     * Returns the best individual of this generation (not the best ever).
     * Only works after sorting.
     *
     * @return best individual of the current generation;
     */
    Individual getCurrentBest () {
        if (members.size() == 0) return null;
        return members.get(0);
    }


    /**
     * Returns the best ever individual of this species.
     *
     * @return best ever individual;
     */
    Individual getBest () {
        return best;
    }


    /**
     * Returns the best fitness of the current generation.
     * Only works after sorting.
     *
     * @return best fitness value of the current generation;
     */
    float getCurrentBestScore () {
        return members.size() == 0 ? 0 : members.get(0).getFitness();
    }


    /**
     * Returns whether this species is stale.
     *
     * @return true if staleness is greater or equal to the threshold;
     */
    boolean isStale () {
        return staleness >= STALENESS_THRESHOLD;
    }


    /**
     * Returns how many member this species currently has.
     *
     * @return size of the members list;
     */
    int numberOfMembers () {
        return members.size();
    }


    /**
     * Clear the list of members of this species.
     */
    void clear () {
        members.clear();
    }

}
