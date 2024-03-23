package nl.tudelft.instrumentation.patching;
import java.util.*;

public class PatchingLab {

        // Academic Comeback

        static Random r = new Random();
        static boolean isFinished = false;
        static String[] currentOperators = {};
        static Map<Integer, Double> operatorTarantulaMap = new HashMap<Integer, Double>();
        static Map<Integer,ArrayList<Integer>> operatorsHit = new HashMap<Integer, ArrayList<Integer>>();

        static void initialize(){
                currentOperators = OperatorTracker.operators;
                for (int i = 0; i < currentOperators.length; i++) {
                        operatorTarantulaMap.put(i, 0.0);
                }
                for (int i = 0; i < currentOperators.length; i++) {
                        operatorsHit.put(i, new ArrayList<Integer>());
                }
        }

        // encounteredOperator gets called for each operator encountered while running tests
        static boolean encounteredOperator(String operator, int left, int right, int operator_nr){
                // Do something useful
                // System.out.println("Operator: " + operator + " left: " + left + " right: " + right + " operator_nr: " + operator_nr);
                // System.out.println(OperatorTracker.checkOutput(OperatorTracker.current_test));
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
                operatorsHit.get(operator_nr).add(OperatorTracker.current_test);

                String replacement = OperatorTracker.operators[operator_nr];
                if(replacement.equals("!=")) return left != right;
                if(replacement.equals("==")) return left == right;
                return false;
        }

        static void computeTarantula(List<Boolean> testResults, int nTests, int nTestsPassed) {
                for (int i = 0; i < currentOperators.length; i++) {
                        int currentOperatorsTrue = 0;
                        int currentOperatorsFalse = 0;
                        for (int j = 0; j < operatorsHit.get(i).size(); j++) {

                                if (testResults.get(operatorsHit.get(i).get(j))) {
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
                        operatorTarantulaMap.put(i, tarantula);
                }
        }

        static void run() {
                initialize();

                // Place the code here you want to run once:
                // You want to change this of course, this is just an example
                // Tests are loaded from resources/rers2020_test_cases. If you are you are using
                // your own tests, make sure you put them in the same folder with the same
                // naming convention.
                List<Boolean> testResults = OperatorTracker.runAllTests();
                int nTests = OperatorTracker.tests.size();
                int nTestsPassed = Collections.frequency(testResults, true);
                System.out.println("Initially, " + nTestsPassed + "/" + nTests
                + " passed. Fitness: " + (double)nTestsPassed/nTests
                );
                computeTarantula(testResults, nTests, nTestsPassed);
                System.out.println("Operator Tarantula Map: " + operatorTarantulaMap);
                // System.out.println("Operators: " + operatorsHit.size());

                // Loop here, running your genetic algorithm until you think it is done
                while (!isFinished) {
                        OperatorTracker.operators = currentOperators;
                        testResults = OperatorTracker.runAllTests();
                        nTests = OperatorTracker.tests.size();
                        nTestsPassed = Collections.frequency(testResults, true);
                        computeTarantula(testResults, nTests, nTestsPassed);
                        System.out.println("After a while, " + nTestsPassed + "/" + nTests
                        + " passed. Fitness: " + (double)nTestsPassed/nTests
                        );
                        System.out.println("Operator Tarantula Map: " + operatorTarantulaMap);
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