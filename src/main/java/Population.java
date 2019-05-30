import java.util.ArrayList;
import java.util.List;
import java.util.Random;


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
    }


    public void updateAliveIndividuals () {
        for (Individual individual : individuals) {
            if (!individual.isAlive()) continue;
            individual.updateSensors();
            individual.think();
            individual.move();
        }
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


    public void printStats () {
        System.out.println();
        System.out.println("Generation: " + generation +
                "  | Number of species: " + species.size() +
                "  | Best score : "       + bestEver.getFitness());

        Genome.printGenome(previousBest.getBrain());
        System.out.println();
    }

}
