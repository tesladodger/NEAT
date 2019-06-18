import com.tesladodger.neat.*;

import java.util.Random;


public class NEATEvolveXOR {

    public static void main (String[] grugs) {


        // Extend the Behavior class.
        class OhBehave implements Behavior {

            private int[] results;

            private OhBehave () {
                results = new int[4];
            }

            public float[][] updateSensors () {
                return new float[][] {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
            }

            public void move (float[][] controls) {
                for (int i = 0; i < controls.length; i++) {
                    this.results[i] = controls[i][0] > .5 ? 1 : 0;
                }
            }

            public boolean solutionFound () {
                return (results[0] == 0) && (results[1] == 1) && results[2] == 1 && results[3] == 0;
            }

            public void render () {}

            public boolean isAlive () {
                return false;
            }

            public float fitnessFunction () {
                float d = 4 - (Math.abs(results[0]    ) +
                               Math.abs(results[1] - 1) +
                               Math.abs(results[2] - 1) +
                               Math.abs(results[3]    ));
                return d*d;
            }

            public OhBehave copy () {
                return new OhBehave();
            }

            public OhBehave copyForReplay () {
                return new OhBehave();
            }
        }


        Random r = new Random();
        Innovation innovation = new Innovation();
        OhBehave behavior = new OhBehave();

        Population population = new Population(2, 1, 500, r, innovation, behavior);
        population.setMode(Population.MODE.FIND_SOLUTION);

        population.saveSolutionToFile("out/xor_solution.txt");
        population.createSolutionImage("out/xor_solution.png");

        // noinspection InfiniteLoopStatement
        while (true) {
            population.updateAliveIndividuals();
            population.naturalSelection(r, innovation);
            population.printStats();
        }

    }

}
