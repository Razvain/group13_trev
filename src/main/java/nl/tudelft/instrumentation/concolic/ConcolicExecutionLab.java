package nl.tudelft.instrumentation.concolic;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.io.FileWriter;
import java.io.IOException;

import com.microsoft.z3.*;


class Pair<A, B> {
    A first;
    B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }
}

/**
 * You should write your solution using this class.
 * 
 * Z3 API: https://z3prover.github.io/api/html/classcom_1_1microsoft_1_1z3_1_1_context.html
 */
public class ConcolicExecutionLab {

    static Random r = new Random();
    static Boolean isFinished = false;
    static List<String> currentTrace;
    static int traceLength = 150;
    static PriorityQueue<Pair<List<String>, Integer>> q = new PriorityQueue<>(Comparator.comparingInt((Pair<List<String>, Integer> pair) -> pair.second).reversed());
    public static Boolean isSatisfiable = false;
    static Set<String> totalBranches = new HashSet<>();
    static Set<String> unsatisfiableBranches = new HashSet<>();
    static Set<String> currentTraceBranches = new HashSet<>();
    static List<Set<String>> previouslyVisitedBranches = new ArrayList<>();
    static int maxxTraceBranches = 0;
    static List<String> best_trace = new ArrayList<>();
    static Set<List<String>> previouslyVisitedTraces = new HashSet<>();
    static long startTime = System.currentTimeMillis();
    static Set<String> errorCodes = new HashSet<>();
    static int currentLine = 0;

    static void initialize(String[] inputSymbols){
        // Initialise a random trace from the input symbols of the problem.
        currentTrace = generateRandomTrace(inputSymbols);
    }

    static MyVar createVar(String name, Expr value, Sort s){
        Context c = PathTracker.ctx;
        /**
         * Create var, assign value and add to path constraint.
         * We show how to do it for creating new symbols, please
         * add similar steps to the functions below in order to
         * obtain a path constraint.
         */
        Expr z3var = c.mkConst(c.mkSymbol(name + "_" + PathTracker.z3counter++), s);
        PathTracker.addToModel(c.mkEq(z3var, value));
        return new MyVar(z3var, name);
    }

    static MyVar createInput(String name, Expr value, Sort s){
        // Create an input var, these should be free variables!
        Context c = PathTracker.ctx;

        String nameA = name + "_" + PathTracker.z3counter++;
        Expr z3var = c.mkConst(c.mkSymbol(nameA), s); // change this line to the correct code for creating a z3var.
        
        // The following code is to add an additional constraint on the input variable.
        // The input variable must have a value that is equal to one of the input symbols.
        BoolExpr constraint = c.mkFalse();
        for (String input: PathTracker.inputSymbols) {
            constraint = c.mkOr(c.mkEq(z3var, c.mkString(input)), constraint);
        }

        PathTracker.addToModel(constraint);
        MyVar myVar = new MyVar(z3var, nameA);
        PathTracker.inputs.add(myVar);

        return myVar;
    }

    static MyVar createBoolExpr(BoolExpr var, String operator){
        if (operator.equals("!")) {
            return new MyVar(PathTracker.ctx.mkNot(var));
        }
        // error("Unsupported unary operator " + operator);
        throw new RuntimeException("Unsupported unary operator " + operator);
    }

    static MyVar createBoolExpr(BoolExpr left_var, BoolExpr right_var, String operator){
        // Handle the following binary operators: &, &&, |, ||
        switch (operator) {
            case "&&":
                // return new MyVar(PathTracker.ctx.mkITE(left_var, right_var, PathTracker.ctx.mkFalse()));
                return new MyVar(PathTracker.ctx.mkAnd(left_var, right_var));
            case "||":
                // return new MyVar(PathTracker.ctx.mkITE(left_var, PathTracker.ctx.mkTrue(), right_var));
                return new MyVar(PathTracker.ctx.mkOr(left_var, right_var));
            case "|":
                return new MyVar(PathTracker.ctx.mkOr(left_var, right_var));
            case "&":
                return new MyVar(PathTracker.ctx.mkAnd(left_var, right_var));
            default:
                throw new RuntimeException("Unsupported binary operator " + operator);
        }
    }

    static MyVar createIntExpr(IntExpr var, String operator){
        switch (operator) {
            case "+":
                // System.out.println(new MyVar(PathTracker.ctx.mkIntConst(var.toString())).z3var);
                return new MyVar(PathTracker.ctx.mkIntConst(var.toString()));
                // return new MyVar(PathTracker.ctx.mkInt(var.toString()));
            case "-": 
                // System.out.println(new MyVar(PathTracker.ctx.mkIntConst("-" + var.toString())).z3var);
                return new MyVar(PathTracker.ctx.mkIntConst("-" + var.toString()));
                // return new MyVar(PathTracker.ctx.mkInt(var.getNumArgs()));
            default:
                throw new RuntimeException("Unsupported unary operator " + operator);
        }
        
    }

    static MyVar createIntExpr(IntExpr left_var, IntExpr right_var, String operator){
        switch (operator) {
            case "+":
                return new MyVar(PathTracker.ctx.mkAdd(left_var, right_var));
            case "-":
                return new MyVar(PathTracker.ctx.mkSub(left_var, right_var));
            case "/":
                return new MyVar(PathTracker.ctx.mkDiv(left_var, right_var));
            case "*":
                return new MyVar(PathTracker.ctx.mkMul(left_var, right_var));
            case "%":
                return new MyVar(PathTracker.ctx.mkMod(left_var, right_var));
            case "^":
                return new MyVar(PathTracker.ctx.mkPower(left_var, right_var));
            case "==":
                return new MyVar(PathTracker.ctx.mkEq(left_var, right_var));
            case "<=":
                return new MyVar(PathTracker.ctx.mkLe(left_var, right_var));
            case "<":
                return new MyVar(PathTracker.ctx.mkLt(left_var, right_var));
            case ">=":
                return new MyVar(PathTracker.ctx.mkGe(left_var, right_var));
            case ">":
                return new MyVar(PathTracker.ctx.mkGt(left_var, right_var));
            default:
               throw new RuntimeException("Unsupported binary operator " + operator);
        }
    }

    static MyVar createStringExpr(SeqExpr left_var, SeqExpr right_var, String operator){
        if (operator.equals("==")) {
            return new MyVar(PathTracker.ctx.mkEq(left_var, right_var));
        }
        throw new RuntimeException("Unsupported binary operator " + operator);
        // return new MyVar(PathTracker.ctx.mkEq(left_var, right_var));
    }

    static void assign(MyVar var, String name, Expr value, Sort s){

        // For single static assignment, whenever you encounter an assignment to an already existing variable
        // you create a new variable and assign it that value such that there is no confusion with the variable's
        // scope
        // Expr z3var = PathTracker.ctx.mkConst(PathTracker.ctx.mkSymbol(name + "_" + PathTracker.z3counter++), s);
        // System.out.println(PathTracker.ctx.mkEq(z3var, value));
        String newName = name + "_" + PathTracker.z3counter++;

        // Create a new Z3 variable with the new name and the given sort
        Expr z3var = PathTracker.ctx.mkConst(PathTracker.ctx.mkSymbol(newName), s);
        PathTracker.addToModel(PathTracker.ctx.mkEq(z3var, value));

        var.name = newName;
        var.z3var = z3var;

        // return new MyVar(PathTracker.ctx.mkEq(PathTracker.ctx.mkConst()))
        // All variable assignments, use single static assignment
    }



    static void encounteredNewBranch(MyVar condition, boolean value, int line_nr){
        // Call the solver
        
        // PathTracker.solve((BoolExpr) condition.z3var, false);
        totalBranches.add("line: " + line_nr + " value: " + value);
        currentTraceBranches.add("line: " + line_nr + " value: " + value);
        if(!unsatisfiableBranches.contains("line: " + line_nr + " value: " + (!value))){
            isSatisfiable = false;
            currentLine = line_nr;
            if (value == true) {
                PathTracker.solve(PathTracker.ctx.mkEq(condition.z3var, PathTracker.ctx.mkFalse()), false);
                PathTracker.addToBranches(PathTracker.ctx.mkEq(condition.z3var, PathTracker.ctx.mkTrue()));
                if (!isSatisfiable) {
                    unsatisfiableBranches.add("line: " + line_nr + " value: " + (!value));
                }
            } else {
                PathTracker.solve(PathTracker.ctx.mkEq(condition.z3var, PathTracker.ctx.mkTrue()), false);
                PathTracker.addToBranches(PathTracker.ctx.mkEq(condition.z3var, PathTracker.ctx.mkFalse()));
                if (!isSatisfiable) {
                        unsatisfiableBranches.add("line: " + line_nr + " value: " + (!value));
                    }
            }
            
        }
        
    }

    static void newSatisfiableInput(LinkedList<String> new_inputs) {
        // Hurray! found a new branch using these new inputs!
        // Remove the extra quotes from the inputs that were find by the solver.
        
        List<String> trimmed_new_inputs = new_inputs.stream()
                .map(s -> s.replaceAll("\"", ""))
                .collect(Collectors.toList());
        // System.out.println("New inputs found: " + trimmed_new_inputs);
        // if (!previouslyVisitedTraces.contains(trimmed_new_inputs)) {
            // previouslyVisitedTraces.add(trimmed_new_inputs);
            // System.out.println(currentTraceBranches.size());
            // q.add(new Pair<>(trimmed_new_inputs, currentTraceBranches.size()));
        // }

        // if(!previouslyVisitedBranches.contains(currentTraceBranches)) {
            // previouslyVisitedBranches.add(currentTraceBranches);
            q.add(new Pair<>(trimmed_new_inputs, currentTraceBranches.size()));
        // }

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
         * more branches using concolic execution. Right now we just generate
         * a complete random sequence using the given input symbols. Please
         * change it to your own code.
         */
        // return generateRandomTrace(inputSymbols);
        List<String> mutatedTrace = new ArrayList<>(currentTrace);
        Random random = new Random();
                
        for (int i = 0; i < random.nextInt(100) + 50; i ++) {

        //   int addIndex = random.nextInt(mutatedTrace.size() + 1);   
          String addedSymbol = inputSymbols[random.nextInt(inputSymbols.length)];
          mutatedTrace.add(addedSymbol);
    }
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
        return trace;
    }

    static void run() {
        initialize(PathTracker.inputSymbols);
        PathTracker.runNextFuzzedSequence(currentTrace.toArray(new String[0]));
        best_trace = currentTrace;
        // Place here your code to guide your fuzzer with its search using Concolic Execution.
        while(!isFinished) {
            // Do things!
            try {
                PathTracker.reset();
                currentTraceBranches.clear();
                if(q.isEmpty()){
                    currentTrace = generateRandomTrace(PathTracker.inputSymbols);
                } else {
                    currentTrace = q.poll().first;
                    // System.out.println("Primul " + currentTrace);
                    // for (int i = 0; i <= 100; i ++) {
                    currentTrace = fuzz(PathTracker.inputSymbols);
                    // System.out.println("Second: " + currentTrace);
                    // }
                }
                PathTracker.runNextFuzzedSequence(currentTrace.toArray(new String[0]));
                System.out.println("totalBranches: " + totalBranches.size());
                // System.out.println(totalBranches.toString());
                if(currentTraceBranches.size() > maxxTraceBranches){
                    maxxTraceBranches = currentTraceBranches.size();
                    System.out.println("maxxTraceBranches: " + maxxTraceBranches);
                    best_trace = currentTrace;
                }

                // System.out.println("Woohoo, looping!");
                // Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void output(String out){
        Pattern pattern = Pattern.compile(".*error_(\\d+).*");
                Matcher matcher = pattern.matcher(out);
                if (matcher.matches()) {
                        String matchedInteger = matcher.group(1);
                        if (!errorCodes.contains(matchedInteger)) {
                                errorCodes.add(matchedInteger);
                                long timestamp = (System.currentTimeMillis() - startTime) / 1000;
                                writeToCSV(timestamp, matchedInteger);
                        }

                }
    }

    private static void writeToCSV(long timestamp, String matchedInteger) {
        try (FileWriter writer = new FileWriter("./error_codes/error_log_concolic_prob11.csv", true)) {
            writer.write(timestamp + "," + matchedInteger + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}