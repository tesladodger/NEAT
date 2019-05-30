# Neuroevolution of Augmenting Topologies

This is a java library for the NEAT algorithm.


## How to use


### Extend the Behavior abstract class

This class contains the methods particular to a problem that must be implemented by the user.

| Method | Description |
|--------|-------------|
|updateSensors() | The returned n*m matrix will be the input to the neural network, n is the number of different input patterns (for example, the XOR problem has 4 different patterns) and m is the number of inputs (XOR has 2 inputs). |
| move(float[][] controls) | The param of the move method is the output of neural network (after sigmoid). This is where the individual acts upon the outputs of the neural network. This is a good place to call a render method in a game. |
| isAlive() | Returns true if the individual is alive, which means it will be updated. Useful for games. |
| fitnessFunction() | Calculates the fitness of an individual. |
| copy() | Must return a completely independant copy of a Behave instance. Make sure it contains new structures instead of references to the old ones. |


### Create instances of Random, Innovation and your class that extends Behavior
```java
Random r = new Random();
Innovation innovation = new Innovation();
Behavior behavior = new OhBehave();
```

### Creat a population
```java
Population population = new Population(i, o, s, r, innov, behav);
```
| Param | Description |
|-------|-------------|
| i | number of inputs to the neural network |
| o | number of outputs from the network |
| s | population size |
| innov | Innovation instance |
| behav | instance of a class that extends Behavior |


```java
Population population = new Population(2, 1, 500, r, innovation, behavior);
```


### Draw or infinite loop

Since the XOR problem only requires one evaluation per generation, an infinite loop can be used (you can specify an exit condition from the Behavior class).

```java
while (true) {
    population.updateAliveIndividuals();
    population.naturalSelection(r, innovation);
    population.printStats();
}
```

In a draw() loop, it would look like this.
```java
void draw () {
    if (population.areAllDead()) {
        population.naturalSelection(r, innov);
    }
    else {
        population.updateAliveIndividuals();
        population.printStats();
    }
}
```
