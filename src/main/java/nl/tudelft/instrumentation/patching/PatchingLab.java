package nl.tudelft.instrumentation.patching;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.checkerframework.checker.units.qual.A;
import java.util.stream.Collectors;

class GenerationIndividual {
        String[] operators;
        double fitness;

        public GenerationIndividual(String[] operators, int fitness) {
                this.operators = operators;
                this.fitness = fitness;
        }
}

public class PatchingLab {

        
        //Paramenters
        static int generationSize = 10;
        static double mutationRate = 1;
        static double crossoverRate = 0.2;
        static int matingPoolSize = 8;
        
        
        //Variables
        static Random r = new Random();
        static boolean isFinished = false;
        static List<GenerationIndividual> population = new ArrayList<GenerationIndividual>();
        static Map<Integer, Double> tarantula = new HashMap<Integer, Double>();
        static Map<Integer, ArrayList<Integer>> operatorsHit = new HashMap<Integer, ArrayList<Integer>>();
        static GenerationIndividual bestIndividual = new GenerationIndividual(new String[]{}, 0);
        static Set<Integer> booleanOperators = new HashSet<Integer>();
        static Set<Integer> faultyOperators = new HashSet<Integer>();
        static long startTime = System.currentTimeMillis();
        static String[] initialOperators = new String[]{};

        static void initialize(){
                
                initialOperators = OperatorTracker.operators;
                for (int i = 0; i < OperatorTracker.operators.length; i++) {
                        tarantula.put(i, 0.0);
                }
                for (int i = 0; i < OperatorTracker.operators.length; i++) {
                        operatorsHit.put(i, new ArrayList<Integer>());
                }

                for (int i = 0; i < generationSize; i++) {
                        population.add(new GenerationIndividual(Arrays.copyOf(OperatorTracker.operators, OperatorTracker.operators.length), 0));
                }
                List<Boolean> testResults = OperatorTracker.runAllTests();
                computeTarantula(testResults);
                for (Map.Entry<Integer, Double> entry : tarantula.entrySet()) { 
                        if (entry.getValue() > 0.9) {
                                faultyOperators.add(entry.getKey());
                        }
                }
                operatorsHit = null;
                tarantula = null;

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
                        List<Boolean> testResults = OperatorTracker.runAllTests();
                        individual.fitness = getFittness(testResults);
                        System.out.println("Individual fitness: " + individual.fitness);
                        if (individual.fitness > bestIndividual.fitness) {
                                bestIndividual = individual;
                                writeBestIndividual();
                        }
                }
                System.gc();
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

        

        // verificam bool vs int 

        static GenerationIndividual mutate(GenerationIndividual individual){
                String[] newOperators = new String[individual.operators.length];

                List<Integer> operatorsToMutate = new ArrayList<Integer>();
                for (int i = 0; i < mutationRate; i++) {
                        int randomIndex = r.nextInt(faultyOperators.size());
                        operatorsToMutate.add(faultyOperators.toArray(new Integer[faultyOperators.size()])[randomIndex]);
                }
                
                for (int i = 0; i < individual.operators.length; i++) {
                        if (operatorsToMutate.contains(i)) {
                                if (booleanOperators.contains(i)) {
                                        newOperators[i] = individual.operators[i].equals("==") ? "!=" : "==";
                                } else {
                                        List<String> possibleOperators = new ArrayList<String>(Arrays.asList("!=", "==", "<", ">", "<=", ">="));
                                        possibleOperators.remove(individual.operators[i]);
                                        newOperators[i] = possibleOperators.get(r.nextInt(possibleOperators.size()));
                                }
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
                        newPopulation.add(mutate(children.get(0)));
                        newPopulation.add(mutate(children.get(1)));
                }
                
                newPopulation.add(bestIndividual);
                population.sort((a, b) -> Double.compare(b.fitness, a.fitness));
                int i = 0;
                while (newPopulation.size() < generationSize) {
                        newPopulation.add(population.get(i));
                        i++;
                }

                population = newPopulation;
        }
        

        // encounteredOperator gets called for each operator encountered while running tests
        static boolean encounteredOperator(String operator, int left, int right, int operator_nr){
                // Do something useful
                if (operatorsHit != null)
                        operatorsHit.get(operator_nr).add(OperatorTracker.current_test);

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
                if (operatorsHit != null)
                        operatorsHit.get(operator_nr).add(OperatorTracker.current_test);
                booleanOperators.add(operator_nr);

                String replacement = OperatorTracker.operators[operator_nr];
                if(replacement.equals("!=")) return left != right;
                if(replacement.equals("==")) return left == right;
                return false;
        }

        static void computeTarantula(List<Boolean> testResults) {
                if (testResults == null || OperatorTracker.operators == null || tarantula == null) {
                        return;
                }

                int nTests = OperatorTracker.tests.size();
                int nTestsPassed = Collections.frequency(testResults, true);
                for (int i = 0; i < OperatorTracker.operators.length; i++) {
                        int currentOperatorsTrue = 0;
                        int currentOperatorsFalse = 0;
                        List<Integer> operatorHits = operatorsHit.get(i);
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
                        double tarantulaVal = ((double)currentOperatorsFalse/(nTests-nTestsPassed)) / ((double)currentOperatorsTrue/nTestsPassed + (double)currentOperatorsFalse/(nTests-nTestsPassed));
                        if (Double.isNaN(tarantulaVal)) {
                                tarantulaVal = 0.0;
                        }
                        tarantula.put(i, tarantulaVal);
                }
        }

        static void run() {
                initialize();

                // Loop here, running your genetic algorithm until you think it is done
                while (!isFinished) {
                        runGeneticAlgorithmOnce();
                        if(bestIndividual.fitness == 1.0){
                                isFinished = true;
                        }
                        writeToCSV((System.currentTimeMillis() - startTime) / 1000, bestIndividual.fitness);
                        System.out.println("Best fitness: " + bestIndividual.fitness);
                }
        }

        public static void output(String out){
                // This will get called when the problem code tries to print things,
                // the prints in the original code have been removed for your convenience

                // System.out.println(out);
        }

        private static void writeToCSV(long timestamp, Double fitness) {
                try (FileWriter writer = new FileWriter("./correct_conv/convergence_graph_prob_15.csv", true)) {
                writer.write(timestamp + "," + fitness + "\n");
                } catch (IOException e) {
                e.printStackTrace();
                }
        }

        private static void writeBestIndividual() {
                try (FileWriter writer = new FileWriter("./correct_operators/best_individual_prob_15.txt", true)) {
                writer.write("Best fitness" + bestIndividual.fitness + " " + Arrays.toString(bestIndividual.operators) + "\n");
                } catch (IOException e) {
                e.printStackTrace();
                }
        }
}