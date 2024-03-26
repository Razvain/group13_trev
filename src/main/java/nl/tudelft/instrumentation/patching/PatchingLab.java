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

        
        //Paramenters
        static int generationSize = 10;
        static double mutationRate = 1;
        static double crossoverRate = 0.8;
        static int matingPoolSize = 8;
        
        
        //Variables
        static Random r = new Random();
        static boolean isFinished = false;
        static String[] currentOperators = {};
        static List<GenerationIndividual> population = new ArrayList<GenerationIndividual>();
        static Map<Integer, Double> currentTarantula = new HashMap<Integer, Double>();
        static Map<Integer, ArrayList<Integer>> currentOperatorsHit = new HashMap<Integer, ArrayList<Integer>>();
        static GenerationIndividual bestIndividual = new GenerationIndividual(new String[]{}, 0);
        static Set<Integer> booleanOperators = new HashSet<Integer>();
        static Set<Integer> faultyOperators = new HashSet<Integer>(); 

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


        static double getFittness(List<Boolean> testResults) {
                int nTests = OperatorTracker.tests.size();
                int nTestsPassed = Collections.frequency(testResults, true);
                System.out.println("Tests passed: " + nTestsPassed + "/" + nTests);
                return (double)nTestsPassed/nTests;
                
        }

        static void runGeneration(){
                for (int i = 0; i < generationSize; i++) {
                        GenerationIndividual individual = population.get(i);
                        OperatorTracker.operators = individual.operators;
                        currentOperatorsHit = individual.operatorsHit;
                        currentTarantula = individual.tarantula;
                        currentOperators = individual.operators;
                        List<Boolean> testResults = OperatorTracker.runAllTests();
                        computeTarantula(testResults);
                        individual.fitness = getFittness(testResults);
                        System.out.println("Individual fitness: " + individual.fitness);
                        if (individual.fitness > bestIndividual.fitness) {
                                bestIndividual = individual;
                                faultyOperators.clear();
                                computeTarantula(testResults);
                                // System.out.println("Tarantula: " + currentTarantula);
                                for (Map.Entry<Integer, Double> entry : currentTarantula.entrySet()) { 
                                        if (entry.getValue() > 0.9) {
                                                faultyOperators.add(entry.getKey());
                                        }
                                } 
                        }
                }
        }

        static void runIndividual(GenerationIndividual individual){
                OperatorTracker.operators = individual.operators;
                currentOperatorsHit = individual.operatorsHit;
                currentTarantula = individual.tarantula;
                currentOperators = individual.operators;
                List<Boolean> testResults = OperatorTracker.runAllTests();
                computeTarantula(testResults);
                individual.fitness = getFittness(testResults);
                System.out.println("Individual fitness: " + individual.fitness);
                if (individual.fitness > bestIndividual.fitness) {
                        bestIndividual = individual;
                }
        }

        static List<GenerationIndividual> roulleteWheelSelection(){
                List<GenerationIndividual> newPopulation = new ArrayList<GenerationIndividual>();
                Double[] fitnessStore = new Double[generationSize];
                for (int i = 0; i < generationSize; i++) {
                        if(population.get(i).fitness == 1.0)
                                fitnessStore[i] = 1.0;
                        else
                                fitnessStore[i] = (double)1/(1-population.get(i).fitness);
                }
                double totalFitness = Arrays.stream(fitnessStore).mapToDouble(Double::doubleValue).sum();
                for (int i = 0; i < generationSize; i++) {
                        fitnessStore[i] /= totalFitness;
                        
                }

                for (int i = 0; i < matingPoolSize; i++) {
                        double random = r.nextDouble();
                        double sum = 0;
                        for (int j = 0; j < generationSize; j++) {
                                sum += fitnessStore[j];
                                if (sum > random || j == generationSize - 1) {
                                        newPopulation.add(population.get(j));
                                        break;
                                }
                        }
                }

                return newPopulation;
        }

        // static List<GenerationIndividual> tournamentSelection(){
        //         Set<GenerationIndividual> newPopulation = new HashSet<GenerationIndividual>();
        //         for (int i = 0; i < generationSize; i++) {
        //                 int randomIndex1 = r.nextInt(generationSize);
        //                 int randomIndex2 = r.nextInt(generationSize);
        //                 GenerationIndividual individual1 = population.get(randomIndex1);
        //                 GenerationIndividual individual2 = population.get(randomIndex2);
        //                 if (individual1.fitness > individual2.fitness) {
        //                         newPopulation.add(individual1);
        //                 } else {
        //                         newPopulation.add(individual2);
        //                 }
        //         }
        //         return new ArrayList<GenerationIndividual>(newPopulation);
        // }
        

        // verificam bool vs int 

        static GenerationIndividual mutate(GenerationIndividual individual){
                String[] newOperators = new String[individual.operators.length];
                // int mutationPoint = (int) (individual.operators.length * mutationRate);
                // int mutationPoint = 5;

                // List<Integer> operatorsToMutate = individual.tarantula.entrySet().stream()
                //         .filter(entry -> entry.getValue() != 0.0)
                //         .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                //         .limit(mutationPoint)
                //         .map(Map.Entry::getKey)
                //         .collect(Collectors.toList());

                // List<Integer> operatorsToMutate = individual.tarantula.entrySet().stream()
                //         .filter(entry -> entry.getValue() > 0.9)
                //         .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                //         .limit(mutationPoint)
                //         .map(Map.Entry::getKey)
                //         .collect(Collectors.toList());

                // System.out.println("Operators to mutate: " + operatorsToMutate);
                // System.out.println("Tarantula: " + individual.tarantula);
                List<Integer> operatorsToMutate = new ArrayList<Integer>();
                for (int i = 0; i < mutationRate; i++) {
                        int randomIndex = r.nextInt(faultyOperators.size());
                        operatorsToMutate.add(faultyOperators.toArray(new Integer[faultyOperators.size()])[randomIndex]);
                }
                
                for (int i = 0; i < individual.operators.length; i++) {
                        if (operatorsToMutate.contains(i)) {
                                // if (booleanOperators.contains(i)) {
                                //         newOperators[i] = individual.operators[i].equals("==") ? "!=" : "==";
                                // } else {
                                List<String> possibleOperators = new ArrayList<String>(Arrays.asList("!=", "==", "<", ">", "<=", ">="));
                                possibleOperators.remove(individual.operators[i]);
                                newOperators[i] = possibleOperators.get(r.nextInt(possibleOperators.size()));
                                // }
                        } else {
                                newOperators[i] = individual.operators[i];
                        }
                }

                return new GenerationIndividual(newOperators, 0);
        }

        static List<GenerationIndividual> crossover(GenerationIndividual parent1, GenerationIndividual parent2){
                int crossoverPoint = (int) (OperatorTracker.operators.length * crossoverRate);
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
                List<GenerationIndividual> selectionResult = roulleteWheelSelection();
                List<GenerationIndividual> newPopulation = new ArrayList<GenerationIndividual>();
                for (int i = 0; i < matingPoolSize / 2; i++) {
                        GenerationIndividual parent1 = selectionResult.get(r.nextInt(selectionResult.size()));
                        GenerationIndividual parent2 = selectionResult.get(r.nextInt(selectionResult.size()));
                        List<GenerationIndividual> children = crossover(parent1, parent2);
                        // runIndividual(children.get(0));
                        // runIndividual(children.get(1));
                        newPopulation.add(mutate(children.get(0)));
                        newPopulation.add(mutate(children.get(1)));
                }
                
                newPopulation.add(bestIndividual);
                //Sort population by fitness reverse
                population.sort((a, b) -> Double.compare(b.fitness, a.fitness));
                int i = 0;
                while (newPopulation.size() < generationSize) {
                        // newPopulation.add(mutate(bestIndividual));
                        newPopulation.add(population.get(i));
                        i++;
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
                booleanOperators.add(operator_nr);

                String replacement = OperatorTracker.operators[operator_nr];
                if(replacement.equals("!=")) return left != right;
                if(replacement.equals("==")) return left == right;
                return false;
        }

        static void computeTarantula(List<Boolean> testResults) {
                if (testResults == null || currentOperators == null || currentTarantula == null) {
                        return; // or throw an exception, depending on your use case
                }

                int nTests = OperatorTracker.tests.size();
                int nTestsPassed = Collections.frequency(testResults, true);
                for (int i = 0; i < currentOperators.length; i++) {
                        int currentOperatorsTrue = 0;
                        int currentOperatorsFalse = 0;
                        List<Integer> operatorHits = currentOperatorsHit.get(i);
                        if (operatorHits == null) {
                                continue;
                        }
                        for (int j = 0; j < operatorHits.size(); j++) {
                                Integer hitIndex = operatorHits.get(j);
                                if (hitIndex == null || hitIndex >= testResults.size()) {
                                        continue;
                                }
                                if (testResults.get(hitIndex)) {
                                        currentOperatorsTrue++;
                                } else {
                                        currentOperatorsFalse++;
                                }
                        }
                        double tarantula = ((double)currentOperatorsFalse/(nTests-nTestsPassed)) / ((double)currentOperatorsTrue/nTestsPassed + (double)currentOperatorsFalse/(nTests-nTestsPassed));
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
                        // runGeneration();
                        System.out.println("Best fitness: " + bestIndividual.fitness);
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