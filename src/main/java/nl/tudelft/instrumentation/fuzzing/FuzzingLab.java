package nl.tudelft.instrumentation.fuzzing;

import java.util.*;

/**
 * You should write your own solution using this class.
 */
public class FuzzingLab {
        static Random r = new Random();
        static List<String> currentTrace;
        static int traceLength = 10;
        static boolean isFinished = false;
        static float distance = 0;
        static float branches = 0;
        static List<Integer> branch_occurrence = new ArrayList<Integer>();
        static List<Float> branch_dist = new ArrayList<Float>();
        static float final_distance = 0;
        static List<String> best_trace;

        static void initialize(String[] inputSymbols){
                // Initialise a random trace from the input symbols of the problem.
                currentTrace = generateRandomTrace(inputSymbols);
        }

        /**
         * Write your solution that specifies what should happen when a new branch has been found.
         */
        static void encounteredNewBranch(MyVar condition, boolean value, int line_nr) {
                // do something useful
                //System.out.println(currentTrace);
                if (!branch_occurrence.contains(line_nr)) {
                        branch_occurrence.add(line_nr);
                        branches += 1;
                        branch_dist.add(calculateDistance(condition, value, line_nr));
                }
                System.out.println("New branches found: " + branches);
                // System.out.println(branch_dist);
                distance += calculateDistance(condition, value, line_nr);
                System.out.println(currentTrace);
                
        }

        static float calculateDistance(MyVar condition, boolean value, int line_nr) {
                // Add your code here to calculate the distance between two traces.
                switch (condition.type.getValue()) {
                        case 1:
                                // Boolean
                                return condition.value == value ? 0 : 1;
                        case 2:
                                // Integer
                                return (float) condition.int_value;
                        case 3:
                                //String
                                int asciiSum = 0;
                                for(int i = 0; i < condition.str_value.length(); i++) {
                                        asciiSum += (int) condition.str_value.charAt(i);
                                }
                                return (float) asciiSum;
                        case 4:
                                //Unary
                                return 1 - normalizeDistance(calculateDistance(condition.left, value, line_nr));
                        case 5:
                                //Binary
                                switch (condition.operator) {
                                        case "==":
                                                return Math.abs(calculateDistance(condition.left, value, line_nr) - calculateDistance(condition.right, value, line_nr));
                                        case "!=":
                                                return calculateDistance(condition.left, value, line_nr) != calculateDistance(condition.right, value, line_nr) ? 0 : 1;
                                        case "<":
                                                return calculateDistance(condition.left, value, line_nr) < calculateDistance(condition.right, value, line_nr) ? 0 : calculateDistance(condition.left, value, line_nr) - calculateDistance(condition.right, value, line_nr) + 3;
                                        case "<=":
                                                return calculateDistance(condition.left, value, line_nr) <= calculateDistance(condition.right, value, line_nr) ? 0 : calculateDistance(condition.left, value, line_nr) - calculateDistance(condition.right, value, line_nr);
                                        case ">":
                                                return calculateDistance(condition.left, value, line_nr) > calculateDistance(condition.right, value, line_nr) ? 0 : calculateDistance(condition.right, value, line_nr) - calculateDistance(condition.left, value, line_nr) + 3;
                                        case ">=":
                                                return calculateDistance(condition.left, value, line_nr) >= calculateDistance(condition.right, value, line_nr) ? 0 : calculateDistance(condition.right, value, line_nr) - calculateDistance(condition.left, value, line_nr);
                                        case "&&":
                                                return normalizeDistance(calculateDistance(condition.left, value, line_nr)) + normalizeDistance(calculateDistance(condition.right, value, line_nr));
                                        case "||":
                                                return Math.min(normalizeDistance(calculateDistance(condition.left, value, line_nr)), normalizeDistance(calculateDistance(condition.right, value, line_nr)));
                                        case "XOR":
                                                return Math.min(normalizeDistance(calculateDistance(condition.left, value, line_nr)) + normalizeDistance(calculateDistance(new MyVar(condition.right, "!"), value, line_nr)), normalizeDistance(calculateDistance(new MyVar(condition.left, "!"), value, line_nr)) + normalizeDistance(calculateDistance(condition.right, value, line_nr)));
                                        default:
                                                return 0;
                                }
                        default:
                                System.out.println("Unknown");
                                return -1;
                }
        }

        static float normalizeDistance(float distance) {
                // Add your code here to normalize the distance between two traces.
                return distance / (distance + 1);
        }

        /**
         * Method for fuzzing new inputs for a program.
         * @param inputSymbols the inputSymbols to fuzz from.
         * @return a fuzzed sequence
         */
        static List<String> fuzz(String[] inputSymbols){
                /*
                 * Add here your code for fuzzing a new sequence for the RERS problem.
                 * You can guide your fuzzer to fuzz "smart" input sequences to cover
                 * more branches. Right now we just generate a complete random sequence
                 * using the given input symbols. Please change it to your own code.
                 */
                //  return generateRandomTrace(inputSymbols);
                // List<String> newTrace = new ArrayList<String>(currentTrace);
                // Random random = new Random();
                // int numMutations = random.nextInt(inputSymbols.length / 2) + 1; 

                // for (int i = 0; i < numMutations; i++) {
                //         int index = random.nextInt(newTrace.size());
                //         String newSymbol = inputSymbols[random.nextInt(inputSymbols.length)];
                //         newTrace.set(index, newSymbol);
                // }
                // currentTrace = newTrace;
                // return newTrace;
                List<String> mutatedTrace = new ArrayList<>(currentTrace);
                Random random = new Random();
                
                
                for (int i = 0; i < 3; i ++) {

                int mutationType = random.nextInt(3);
                switch (mutationType) {
                        case 0: // Changing a symbol
                        if (!mutatedTrace.isEmpty()) {
                        int changeIndex = random.nextInt(mutatedTrace.size());
                        String newSymbol = inputSymbols[random.nextInt(inputSymbols.length)];
                        mutatedTrace.set(changeIndex, newSymbol); }
                        break;
                        case 1: // Adding a symbol
                        if (!mutatedTrace.isEmpty()) {
                        int addIndex = random.nextInt(mutatedTrace.size() + 1); 
                        String addedSymbol = inputSymbols[random.nextInt(inputSymbols.length)];
                        mutatedTrace.add(addIndex, addedSymbol);
                        }
                        break;
                        case 2: // Deleting a symbol
                        if (!mutatedTrace.isEmpty()) {
                                int deleteIndex = random.nextInt(mutatedTrace.size());
                                mutatedTrace.remove(deleteIndex);
                        }
                        break;
                        default:
                        break;
                }
        }
                // currentTrace = mutatedTrace;
                return mutatedTrace;

        }

        /**
         * Generate a random trace from an array of symbols.
         * @param symbols the symbols from which a trace should be generated from.
         * @return a random trace that is generated from the given symbols.
         */
        static List<String> generateRandomTrace(String[] symbols) {
                ArrayList<String> trace = new ArrayList<>();
                for (int i = 0; i < traceLength; i++) {
                        trace.add(symbols[r.nextInt(symbols.length)]);
                }
                // currentTrace = trace;
                return trace;
        }

        static void run() {
                initialize(DistanceTracker.inputSymbols);
                DistanceTracker.runNextFuzzedSequence(currentTrace.toArray(new String[0]));
                final_distance = distance;
                best_trace = currentTrace;
                // Place here your code to guide your fuzzer with its search.
                while(!isFinished) {
                        // Do things!
                        try {
                                boolean status = false;
                                List<String> storeTrace;
                                for (int i = 0; i < 5; i++) {
                                        storeTrace = fuzz(DistanceTracker.inputSymbols);
                                        DistanceTracker.runNextFuzzedSequence(storeTrace.toArray(new String[0]));
                                        if (distance < final_distance) {
                                                final_distance = distance;
                                                status = true;
                                                best_trace = storeTrace;
                                        }
                                }

                                if (status == false) {
                                        currentTrace = generateRandomTrace(DistanceTracker.inputSymbols);
                                } else {
                                        currentTrace = best_trace;
                                }
                                
                                // if (distance <= final_distance) {
                                //         final_distance = distance;
                                //         previousTrace = currentTrace;
                                // } else {
                                //         currentTrace = previousTrace;
                                // }

                                System.out.println("Woohoo, looping!");
                                Thread.sleep(1000);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                }
        }

        /**
         * Method that is used for catching the output from standard out.
         * You should write your own logic here.
         * @param out the string that has been outputted in the standard out.
         */
        public static void output(String out){
                System.out.println(out);
        }
}
