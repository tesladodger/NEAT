import java.util.Random;


public class NEATEvolveXOR {

    public static void main (String[] grugs) {


        // Extend the Behavior class.
        class OhBehave extends Behavior {

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

            public boolean isAlive () {
                return false;
            }

            public float fitnessFunction () {
                float d = 4 - (Math.abs(results[0]    ) +
                               Math.abs(results[1] - 1) +
                               Math.abs(results[2] - 1) +
                               Math.abs(results[3]    ));
                if (d == 4) {
                    System.out.println("\nSolution found");
                    for (int i : results) {
                        System.out.print(i);
                    }
                    System.exit(0);
                }
                return d*d;
            }

            public OhBehave copy () {
                return new OhBehave();
            }

        }


        Random r = new Random();
        Innovation innovation = new Innovation();
        OhBehave behavior = new OhBehave();

        Population population = new Population(2, 1, 500, r, innovation, behavior);

        // noinspection InfiniteLoopStatement
        while (true) {
            population.updateAliveIndividuals();
            population.naturalSelection(r, innovation);
            population.printStats();
        }


    }

}
