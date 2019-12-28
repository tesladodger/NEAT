package com.tesladodger.neat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// todo
//      - getter for the genome as a list, in order to render it in a game
//      - find way to import the csv (that's the easy part) and start from it (that's gonna be a pain in the ass)
//      - import just one individual and start from it or import the entire population

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

    /* Different modes the simulation can be run. */
    public enum MODE {
        /*
         * Update the population step (frame) by step, perform natural selection when they are
         * all dead.
         */
        NORMAL,

        /*
         * Run the entire simulation for a generation without rendering, clone the best individual
         * and replay it.
         */
        ONLY_SHOW_BEST,

        /*
         * When the problem has a known solution. Prints the genome of the solution and, when
         * applicable, creates a csv file and image of the solution.
         */
        FIND_SOLUTION,
        ;
    }

    private MODE mode;

    /* In ONLY_SHOW_BEST mode, this is the score of the individual being replayed. */
    private float expectedScore;

    /* In FIND_SOLUTION mode, is set true when the solution is found. */
    private boolean solutionFound;

    /* The user can save the genome to a file in FIND_SOLUTION mode when this boolean is true. */
    private boolean saveToFile;

    /* File name used when saving a genome to a file. */
    private String genomeFileName;

    /* The user can create an image of the solution genome in FIND_SOLUTION mode when this boolean
     * is true. */
    private boolean saveToImage;

    /* File name used when creating the image of a genome. */
    private String imageFileName;

    /* For multithreaded update of the individuals. */
    private int threads;


    /**
     * Constructor with default single threaded update.
     *
     * @param numSensors genome param;
     * @param numControls genome param;
     * @param popSize number of individuals;
     * @param r Random;
     * @param innovation innovation number generator;
     * @param behavior implementation of the behavior interface;
     */
    public Population (int numSensors, int numControls, int popSize,
                       Random r, Innovation innovation, Behavior behavior) {
        this(numSensors, numControls, popSize, r, innovation, behavior, 1);
    }


    /**
     * Constructor.
     *
     * @param numSensors genome param;
     * @param numControls genome param;
     * @param popSize number of individuals;
     * @param r Random;
     * @param innovation innovation number generator;
     * @param behavior implementation of the Behavior interface;
     * @param threads number of threads to be created when updating the simulation;
     */
    public Population (int numSensors, int numControls, int popSize,
                       Random r, Innovation innovation, Behavior behavior,
                       int threads) {

        this.popSize = popSize;

        individuals = new Individual[popSize];
        for (int i = 0; i < popSize; i++) {
            individuals[i] = new Individual(
                    new Genome(numSensors, numControls, false),
                    numSensors, numControls, behavior.copy()
            );
            // This will fully connect the initial nodes.
            individuals[i].getBrain().mutate(r, innovation);
        }

        species = new ArrayList<>();
        generation = 0;

        mode = MODE.NORMAL;
        saveToFile = false;
        saveToImage = false;

        this.threads = threads;
    }


    /**
     * Step by step simulation. Updates and renders all alive individuals.
     *
     * In FIND_SOLUTION mode, prints the corresponding genome and, if applicable, calls the methods
     * to save it to a file and an image.
     *
     * @throws InvalidModeException when called in ONLY_SHOW_BEST mode;
     */
    public void updateAliveIndividuals () {
        if (mode == MODE.ONLY_SHOW_BEST) throw new InvalidModeException("Step by step simulation is" +
                " not available in ONLY_SHOW_BEST mode.");

        MultiThreadedUpdate multiThreadedUpdate = new MultiThreadedUpdate(individuals, threads);
        multiThreadedUpdate.compute();

        /* Rendering needs to be single-threaded to work in Processing. */
        for (Individual i : individuals) {
            if (i.isAlive()) i.render();
        }

        if (mode != MODE.FIND_SOLUTION) return;

        for (Individual i : individuals) {
            if (i.isSolution()) {
                System.out.println("\nSolution found in " + generation + " generations:");
                Genome.printlnGenome(i.getBrain());
                if (saveToFile) {
                    Genome.saveGenome(i.getBrain(), genomeFileName);
                }
                if (saveToImage) {
                    Genome.saveImage(i.getBrain(), imageFileName);
                }
                solutionFound = true;
            }
        }
    }


    /**
     * In FIND_SOLUTION mode, returns whether the solution has been found.
     *
     * @return true if the solution has been found, false otherwise;
     *
     * @throws InvalidModeException when called outside FIND_SOLUTION mode;
     */
    public boolean isSolutionFound () {
        if (mode != MODE.FIND_SOLUTION) throw new InvalidModeException("Method cannot be called outside" +
                "FIND_SOLUTION mode.");

        return solutionFound;
    }


    /**
     * Background simulation. Runs the simulation and calls natural selection. Makes a replay copy
     * of the best of this generation.
     *
     * Should be called once before the render loop.
     *
     * @param r Random;
     * @param innovation generator;
     *
     * @throws InvalidModeException when called outside ONLY_SHOW_BEST mode;
     */
    public void runSimulation (Random r, Innovation innovation) {
        if (mode != MODE.ONLY_SHOW_BEST) throw new InvalidModeException("Background simulation is " +
                "only available in ONLY_SHOW_BEST mode.");

        while (!areAllDead()) {
            for (Individual i : individuals) {
                MultiThreadedUpdate multiThreadedUpdate = new MultiThreadedUpdate(individuals, threads);
                multiThreadedUpdate.compute();
            }
        }

        naturalSelection(r, innovation);
        previousBestReplayCopy = previousBest.copyForReplay();
        expectedScore = previousBest.getFitness();
    }


    /**
     * Runs the simulation on a copy of the best of the previous generation.
     *
     * @throws RuntimeException when called after the replay individual is dead;
     * @throws InvalidModeException when called outside ONLY_SHOW_BEST mode;
     */
    public void updatePreviousBestReplay () {
        if (!replayIndividualIsAlive()) throw new RuntimeException("Replay called on dead " +
                "individual.");
        if (mode != MODE.ONLY_SHOW_BEST) throw new InvalidModeException("Replay is only available " +
                "in ONLY_SHOW_BEST mode.");

        previousBestReplayCopy.updateSensors();
        previousBestReplayCopy.think();
        previousBestReplayCopy.move();
        previousBestReplayCopy.render();
    }


    /**
     * When in ONLY_SHOW_BEST mode, use this to check if still replaying or the simulation should
     * be rerun.
     *
     * @return true if the replay individual is dead. In that case, the simulation should be rerun;
     *
     * @throws InvalidModeException when called outside ONLY_SHOW_BEST mode;
     */
    public boolean replayIndividualIsAlive () {
        if (mode != MODE.ONLY_SHOW_BEST) throw new InvalidModeException("Access to replay individual " +
                "is only permitted in ONLY_SHOW_BEST mode.");

        return previousBestReplayCopy.isAlive();
    }


    /**
     * Returns whether or not all the individuals are dead.
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
     *
     * @param r random;
     * @param innovation innovation generator;
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
        if (index < nextGen.length) {
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
     * Getter for the expected score when running ONLY_SHOW_BEST mode.
     *
     * @return score of the previous best;
     *
     * @throws InvalidModeException when called outside ONLY_SHOW_BEST mode;
     */
    public float getExpectedScore () {
        if (mode != MODE.ONLY_SHOW_BEST) throw new InvalidModeException("Expected score only " +
                "available in ONLY_SHOW_BEST mode.");

        return expectedScore;
    }


    /**
     * Change the mode of this population.
     *
     * @param mode see MODE enum;
     */
    public void setMode (MODE mode) {
        this.mode = mode;
    }


    /**
     * Choose to save the solution in FIND_SOLUTION mode to a file.
     *
     * @param genomeFileName name of the save file;
     *
     * @throws InvalidModeException when called outside FIND_SOLUTION mode;
     */
    public void saveSolutionToFile (String genomeFileName) {
        if (mode != MODE.FIND_SOLUTION) throw new InvalidModeException("Saving the solution is only" +
                "available in FIND_SOLUTION mode.");

        saveToFile = true;
        this.genomeFileName = genomeFileName;
    }


    /**
     * Choose to create an image of the solution in FIND_SOLUTION mode.
     *
     * @param imageFileName name of the save image;
     *
     * @throws InvalidModeException when called outside FIND_SOLUTION mode;
     */
    public void createSolutionImage (String imageFileName) {
        if (mode != MODE.FIND_SOLUTION) throw new InvalidModeException("Creating an image of the" +
                "solution is only available in FIND_SOLUTION mode.");

        saveToImage = true;
        this.imageFileName = imageFileName;
    }


    /*
     * Prints the generation, number of species, best fitness score and calls the printGenome
     * method on the previous best genome.

    public void printStats () {
        System.out.println();
        System.out.println("Generation: " + generation +
                "  | Number of species: " + species.size() +
                "  | Best score : "       + bestEver.getFitness());

        Genome.printGenome(previousBest.getBrain());
        System.out.println();
    }*/

    public int getGeneration () {
        return generation;
    }

    public int getNumberSpecies () {
        return species.size();
    }

    public float getBestScore () {
        return bestEver.getFitness();
    }

    public void printPreviousBestGenome () {
        Genome.printlnGenome(previousBest.getBrain());
    }

}
