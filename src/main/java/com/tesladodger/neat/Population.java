package com.tesladodger.neat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// todo getter for the genome as a list, in order to render it in a game
// todo expected fitness in only_show_best mode
// todo print the genome like the paper

/**
 * Collection of individuals separated into species.
 * Is responsible maintaining and evolving the species.
 */
public class Population {

    /* Array of all the individuals in a generation. */
    private Individual[] individuals;

    /* Number of individuals. */
    private int popSize;

    /* List of species. */
    private List<Species> species;

    /* Counter for number of generations. */
    private int generation;

    /* Best individual of all generations. */
    private Individual bestEver;

    /* Best individual of the previous generation. */
    private Individual previousBest;
    private Individual previousBestReplayCopy;

    /* Option to run the simulation internally and run the simulation again on the best individual. */
    private boolean only_show_best;


    /**
     * Constructor.
     *
     * @param numberSensors genome param;
     * @param numberControls genome param;
     * @param popSize number of individuals;
     * @param r Random;
     * @param innovation innovation number generator;
     */
    public Population (int numberSensors, int numberControls, int popSize,
                       Random r, Innovation innovation, Behavior behavior) {

        this.popSize = popSize;

        individuals = new Individual[popSize];
        for (int i = 0; i < popSize; i++) {
            individuals[i] = new Individual(
                    new Genome(numberSensors, numberControls, false),
                    numberSensors,
                    numberControls,
                    behavior.copy()
            );
            individuals[i].getBrain().mutate(r, innovation);
        }

        species = new ArrayList<>();
        generation = 0;

        only_show_best = false;
    }


    /**
     * Step by step simulation. Updates and renders all alive individuals.
     */
    public void updateAliveIndividuals () {
        if (only_show_best) throw new RuntimeException("Step by step simulation is not available in " +
                "only_show_best mode.");

        for (Individual i : individuals) {
            if (!i.isAlive()) continue;
            i.updateSensors();
            i.think();
            i.move();
            i.render();
        }
    }


    /**
     * Background simulation. Runs the simulation and calls natural selection. Makes a replay copy
     * of the best of this generation.
     *
     * Should be called once before the render loop.
     *
     * @param r Random;
     * @param innovation generator;
     */
    public void runSimulation (Random r, Innovation innovation) {
        if (!only_show_best) throw new RuntimeException("Background simulation is only available in " +
                "only_show_best mode.");

        long c = 0;
        while (!areAllDead()) {
            //if (c > 1000000000) {
            //    System.out.println("Simulation loop too long, maybe a solution was found.");
            //    break;
            //}
            for (Individual i : individuals) {
                if(!i.isAlive()) continue;
                i.updateSensors();
                i.think();
                i.move();
            }
            //System.out.println(c++);
        }

        naturalSelection(r, innovation);
        previousBestReplayCopy = previousBest.copyForReplay();
    }


    /**
     * Runs the simulation on a copy of the best of the previous generation.
     */
    public void replayPreviousBest () {
        if (!replayIndividualIsAlive()) throw new RuntimeException("Replay called on " +
                "dead individual.");
        if (!only_show_best) throw new RuntimeException("Replay is only available in only_show_best " +
                "mode.");

        previousBestReplayCopy.updateSensors();
        previousBestReplayCopy.think();
        previousBestReplayCopy.move();
        previousBestReplayCopy.render();
    }


    /**
     * When in only_show_best mode, use this to check if still replaying or the simulation should
     * be rerun.
     *
     * @return true if the replay individual is dead; In that case, the simulation should be rerun;
     */
    public boolean replayIndividualIsAlive () {
        if (!only_show_best) throw new RuntimeException("Access to replay individual is only " +
                "permitted in only_show_best mode.");

        return previousBestReplayCopy.isAlive();
    }


    /**
     * Returns whether or not all the individuals are dead (only useful for games).
     *
     * @return true if all individuals are dead;
     */
    public boolean areAllDead () {
        for (Individual i : individuals) {
            if (i.isAlive()) {
                return false;
            }
        }
        return true;
    }


    /**
     * Creates the next generation.
     */
    public void naturalSelection (Random r, Innovation innovation) {
        speciate();
        for (Species s : species) {
            s.calculateIndividualFitnesses();  // Calculate the fitness of all individuals.
            s.sort();  // Sort the members by their fitness.
        }
        sortSpecies();  // Sort the species by their fitness.

        previousBest = species.get(0).getCurrentBest().copy();
        if (bestEver == null) bestEver = previousBest.copy();
        if (species.get(0).getCurrentBest().getFitness() > bestEver.getFitness()) {
            bestEver = species.get(0).getCurrentBest().copy();
        }

        for (Species s : species) {
            s.normalizeFitness();  // Divide fitness by number of members (fitness sharing).
            s.calculateAdjustedFitnessSum();  // Sum of the adjusted fitnesses of the members of a species.
            s.thanos();  // Kill half the population.
        }
        killStaleSpecies();
        float aveSum = calculateAverageFitnessSum();
        killUnreproducibleSpecies(aveSum);

        // Build the next generation.
        Individual[] nextGen = new Individual[popSize];
        int index = 0;  // Current index to add to nextGen.

        for (Species s : species) {
            // Add the best of every species without any mutation.
            if (s.numberOfMembers() > 5) {
                nextGen[index++] = s.getBest().copy();
            }

            int allowedChildren = (int) Math.floor((s.getAdjustedFitnessSum() / aveSum) * popSize) - 1;
            for (int i = 0; i < allowedChildren; i++) {
                nextGen[index++] = s.makeAChild(r, innovation);
            }
        }

        // Add a copy of the best for good luck.
        if (index < nextGen.length-1) {
            nextGen[index++] = previousBest.copy();
        }

        // If the next generation is still not full, keep adding children from the best species.
        while (index < nextGen.length) {
            nextGen[index++] = species.get(0).makeAChild(r, innovation);
        }

        individuals =  nextGen;
        generation++;
    }


    /**
     * Divide the population into species.
     */
    private void speciate () {
        // Clear the members of every species.
        for (Species s : species) {
            s.clear();
        }

        // Go through all the individuals.
        for (Individual individual : individuals) {
            boolean speciesFound = false;
            for (Species s : species) {
                // When a species can accept this individual, add it.
                if (s.canAccept(individual.getBrain())) {
                    s.addToSpecies(individual);
                    speciesFound = true;
                    break;
                }
            }
            // If no species is found, create a new one.
            if (!speciesFound) {
                species.add(new Species(individual));
            }
        }
    }


    /**
     * Sorts the current list of species by the fitness of their best individual.
     * Only works after sorting every species.
     */
    private void sortSpecies () {
        int i = 1;
        while (i < species.size()) {
            Species si = species.get(i);
            int j = i - 1;
            while (j >= 0 && species.get(j).getCurrentBestScore() < si.getCurrentBestScore()) {
                species.set(j+1, species.get(j));
                j--;
            }
            species.set(j+1, si);
            i++;
        }
    }


    /**
     * Removes stale species from the list, always keeping the best 3.
     */
    private void killStaleSpecies () {
        for (int i = species.size()-1; i > 2; i--) {
            if (species.get(i).isStale()) {
                species.remove(i);
            }
        }
    }


    /**
     * Removes species whose average fitness is too low to make children.
     *
     * @param aveSum sum of all average fitnesses of the species;
     */
    private void killUnreproducibleSpecies (float aveSum) {
        for (int i = species.size()-1; i > 0; i--) {
            if ((species.get(i).getAdjustedFitnessSum() / aveSum) * individuals.length < 1) {
                species.remove(i);
            }
        }
    }


    /**
     * Calculates the sum of the average fitnesses of the species.
     *
     * @return sum of the average fitnesses;
     */
    private float calculateAverageFitnessSum () {
        float sum = 0;
        for (Species s : species) {
            sum += s.getAdjustedFitnessSum();
        }
        return sum;
    }


    /**
     * Calculates how many individuals are still alive in the population.
     *
     * @return number of alive individuals;
     */
    public int getNumberOfAliveIndividuals () {
        int count = 0;
        for (Individual i : individuals) {
            if (i.isAlive()) count++;
        }
        return count;
    }


    /**
     * Configuration method to only show the best individual of a generation.
     *
     * @param only_show_best true to set the option, false to reset it;
     */
    public void setOnly_show_best (boolean only_show_best) {
        this.only_show_best = only_show_best;
    }


    public void printStats () {
        System.out.println();
        System.out.println("Generation: " + generation +
                "  | Number of species: " + species.size() +
                "  | Best score : "       + bestEver.getFitness());

        Genome.printGenome(previousBest.getBrain());
        System.out.println();
    }

}
