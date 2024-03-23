package nl.tudelft.instrumentation.patching;
import java.util.*;

import org.checkerframework.checker.units.qual.A;
import java.util.stream.Collectors;

class GenerationIndividual {
        String[] operators;
        double fitness;
        Map<Integer, Double> tarantula = new HashMap<Integer, Double>();
        Map<Integer, ArrayList<Integer>> operatorsHit = new HashMap<Integer, ArrayList<Integer>>();

        public GenerationIndividual(String[] operators, int fitness) {
                this.operators = operators;
                this.fitness = fitness;
                for (int i = 0; i < operators.length; i++) {
                        operatorsHit.put(i, new ArrayList<Integer>());
                        tarantula.put(i, 0.0);
                }
        }

        public GenerationIndividual(String[] operators, int fitness, Map<Integer, Double> tarantula, Map<Integer, ArrayList<Integer>> operatorsHit) {
                this.operators = operators;
                this.fitness = fitness;
                this.tarantula = tarantula;
                this.operatorsHit = operatorsHit;
        }

        public void reset() {
                for (int i = 0; i < operators.length; i++) {
                        operatorsHit.put(i, new ArrayList<Integer>());
                        tarantula.put(i, 0.0);
                }
        }
}

public class PatchingLab {

        // Academic Comeback

        static Random r = new Random();
        static boolean isFinished = false;
        static String[] currentOperators = {};
        static int generationSize = 10;
        static List<GenerationIndividual> population = new ArrayList<GenerationIndividual>();
        static Map<Integer, Double> currentTarantula = new HashMap<Integer, Double>();
        static Map<Integer, ArrayList<Integer>> currentOperatorsHit = new HashMap<Integer, ArrayList<Integer>>();
        static int mutationRate = 10;
        static String[] possibleOperators = {"!=", "==", "<", ">", "<=", ">="};
        static double bestFitness = 0;

        static void initialize(){

                for (int i = 0; i < generationSize; i++) {
                        String[] newOperators = new String[OperatorTracker.operators.length];
                        for (int j = 0; j < OperatorTracker.operators.length; j++) {
                                newOperators[j] = OperatorTracker.operators[j];
                        }
                        population.add(new GenerationIndividual(newOperators, 0));
                }

                // currentOperators = OperatorTracker.operators;
                // for (int i = 0; i < currentOperators.length; i++) {
                //         operatorTarantulaMap.put(i, 0.0);
                // }
                // for (int i = 0; i < currentOperators.length; i++) {
                //         operatorsHit.put(i, new ArrayList<Integer>());
                // }
                // for (int i = 0; i < generationSize; i++) {
                //         String[] newOperators = new String[currentOperators.length];

                //         for (int j = 0; j < currentOperators.length; j++) {
                //                 newOperators[j] = currentOperators[j];
                //         }
                //         population.add(newOperators);
                //         populationFitness.put(i, 0);
                // }
        }

        static void runGeneration(){
                for (int i = 0; i < generationSize; i++) {
                        GenerationIndividual individual = population.get(i);
                        OperatorTracker.operators = individual.operators;
                        currentOperatorsHit = individual.operatorsHit;
                        currentTarantula = individual.tarantula;
                        currentOperators = individual.operators;
                        List<Boolean> testResults = OperatorTracker.runAllTests();
                        int nTests = OperatorTracker.tests.size();
                        int nTestsPassed = Collections.frequency(testResults, true);
                        computeTarantula(testResults, nTests, nTestsPassed);
                        individual.fitness = (double)nTestsPassed/nTests;
                        System.out.println("Individual fitness: " + individual.fitness);
                        if (individual.fitness > bestFitness) {
                                bestFitness = individual.fitness;
                        }
                }
        }

        static List<GenerationIndividual> tournamentSelection(){
                Set<GenerationIndividual> newPopulation = new HashSet<GenerationIndividual>();
                for (int i = 0; i < generationSize; i++) {
                        int randomIndex1 = r.nextInt(Math.min(generationSize, population.size()));
                        int randomIndex2 = r.nextInt(Math.min(generationSize, population.size()));
                        GenerationIndividual individual1 = population.get(randomIndex1);
                        GenerationIndividual individual2 = population.get(randomIndex2);
                        if (individual1.fitness > individual2.fitness) {
                                newPopulation.add(individual1);
                        } else {
                                newPopulation.add(individual2);
                        }
                }
                return new ArrayList<GenerationIndividual>(newPopulation);
        }

        static GenerationIndividual mutate(GenerationIndividual individual){
                String[] newOperators = new String[individual.operators.length];

                List<Integer> operatorsToMutate = individual.tarantula.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(mutationRate).map(Map.Entry::getKey).collect(Collectors.toList());

                for (int i = 0; i < individual.operators.length; i++) {
                        if (operatorsToMutate.contains(i)) {
                                newOperators[i] = possibleOperators[r.nextInt(possibleOperators.length)];
                        } else {
                                newOperators[i] = individual.operators[i];
                        }
                }

                return new GenerationIndividual(newOperators, 0);
                // for (int i = 0; i < individual.operators.length; i++) {
                //         if (r.nextInt(100) < mutationRate) {
                //                 newOperators[i] = OperatorTracker.operators[r.nextInt(OperatorTracker.operators.length)];
                //         } else {
                //                 newOperators[i] = individual.operators[i];
                //         }
                // }
                // return new GenerationIndividual(newOperators, 0);
        }

        static List<GenerationIndividual> crossover(GenerationIndividual parent1, GenerationIndividual parent2){
                int crossoverPoint = r.nextInt(OperatorTracker.operators.length);
                String[] child1Operators = new String[OperatorTracker.operators.length];
                String[] child2Operators = new String[OperatorTracker.operators.length];
                for (int i = 0; i < OperatorTracker.operators.length; i++) {
                        if (i < crossoverPoint) {
                                child1Operators[i] = parent1.operators[i];
                                child2Operators[i] = parent2.operators[i];
                        } else {
                                child1Operators[i] = parent2.operators[i];
                                child2Operators[i] = parent1.operators[i];
                        }
                }
                GenerationIndividual child1 = new GenerationIndividual(child1Operators, 0);
                GenerationIndividual child2 = new GenerationIndividual(child2Operators, 0);
                return Arrays.asList(child1, child2);
        }

        static void runGeneticAlgorithmOnce(){
                runGeneration();
                List<GenerationIndividual> tournamentResult = tournamentSelection();
                List<GenerationIndividual> newPopulation = new ArrayList<GenerationIndividual>();
                for (int i = 0; i < generationSize; i++) {
                        GenerationIndividual parent1 = tournamentResult.get(r.nextInt(tournamentResult.size()));
                        GenerationIndividual parent2 = tournamentResult.get(r.nextInt(tournamentResult.size()));
                        List<GenerationIndividual> children = crossover(parent1, parent2);
                        newPopulation.add(mutate(children.get(0)));
                        newPopulation.add(mutate(children.get(1)));
                }
                population = newPopulation;
        }
        

        // encounteredOperator gets called for each operator encountered while running tests
        static boolean encounteredOperator(String operator, int left, int right, int operator_nr){
                // Do something useful
                // System.out.println("Operator: " + operator + " left: " + left + " right: " + right + " operator_nr: " + operator_nr);
                // System.out.println(OperatorTracker.checkOutput(OperatorTracker.current_test));
                currentOperatorsHit.get(operator_nr).add(OperatorTracker.current_test);

                String replacement = OperatorTracker.operators[operator_nr];
                if(replacement.equals("!=")) return left != right;
                if(replacement.equals("==")) return left == right;
                if(replacement.equals("<")) return left < right;
                if(replacement.equals(">")) return left > right;
                if(replacement.equals("<=")) return left <= right;
                if(replacement.equals(">=")) return left >= right;
                return false;
        }

        static boolean encounteredOperator(String operator, boolean left, boolean right, int operator_nr){
                // Do something useful
                currentOperatorsHit.get(operator_nr).add(OperatorTracker.current_test);

                String replacement = OperatorTracker.operators[operator_nr];
                if(replacement.equals("!=")) return left != right;
                if(replacement.equals("==")) return left == right;
                return false;
        }

        static void computeTarantula(List<Boolean> testResults, int nTests, int nTestsPassed) {
                for (int i = 0; i < currentOperators.length; i++) {
                        int currentOperatorsTrue = 0;
                        int currentOperatorsFalse = 0;
                        for (int j = 0; j < currentOperatorsHit.get(i).size(); j++) {

                                if (testResults.get(currentOperatorsHit.get(i).get(j))) {
                                        currentOperatorsTrue++;
                                } else {
                                        currentOperatorsFalse++;
                                }
                        }
                        double tarantula = ((double)currentOperatorsFalse/(nTests-nTestsPassed)) / ((double)currentOperatorsTrue/nTestsPassed + (double)currentOperatorsFalse/(nTests-nTestsPassed));
                        // NaN means that the operator was never hit
                        // Maybe we need to do smth about this
                        if (Double.isNaN(tarantula)) {
                                tarantula = 0.0;
                        }
                        currentTarantula.put(i, tarantula);
                }
        }

        static void run() {
                initialize();

                // Place the code here you want to run once:
                // You want to change this of course, this is just an example
                // Tests are loaded from resources/rers2020_test_cases. If you are you are using
                // your own tests, make sure you put them in the same folder with the same
                // naming convention.
                // List<Boolean> testResults = OperatorTracker.runAllTests();
                // int nTests = OperatorTracker.tests.size();
                // int nTestsPassed = Collections.frequency(testResults, true);
                // System.out.println("Initially, " + nTestsPassed + "/" + nTests
                // + " passed. Fitness: " + (double)nTestsPassed/nTests
                // );
                // computeTarantula(testResults, nTests, nTestsPassed);
                // System.out.println("Operator Tarantula Map: " + operatorTarantulaMap);
                // System.out.println("Operators: " + operatorsHit.size());

                // Loop here, running your genetic algorithm until you think it is done
                while (!isFinished) {
                        runGeneticAlgorithmOnce();
                        System.out.println("Best fitness: " + bestFitness);
                        // OperatorTracker.operators = currentOperators;
                        // testResults = OperatorTracker.runAllTests();
                        // nTests = OperatorTracker.tests.size();
                        // nTestsPassed = Collections.frequency(testResults, true);
                        // computeTarantula(testResults, nTests, nTestsPassed);
                        // System.out.println("After a while, " + nTestsPassed + "/" + nTests
                        // + " passed. Fitness: " + (double)nTestsPassed/nTests
                        // );
                        // System.out.println("Operator Tarantula Map: " + operatorTarantulaMap);
                        // Do things!
                        try {
                                System.out.println("Woohoo, looping!");
                                Thread.sleep(1000);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                }
        }

        public static void output(String out){
                // This will get called when the problem code tries to print things,
                // the prints in the original code have been removed for your convenience

                // System.out.println(out);
        }
}