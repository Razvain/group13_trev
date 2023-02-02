package nl.tudelft.instrumentation.learning;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

/**
 * @author Bram Verboom
 */

public class MealyMachine extends SystemUnderLearn {

    private MealyState initialState;

    public MealyMachine(MealyState initialState) {
        this.initialState = initialState;
    }

    public String[] getOutput(String[] trace) {
        MealyState s = initialState;
        List<String> output = new ArrayList<>();
        for (String sym : trace) {
            MealyTransition t = s.next(sym);
            output.add(t.output);
            s = t.to;
        }
        assert output.size() == trace.length;
        return output.toArray(String[]::new);
    }

    public Set<MealyState> getStates() {
        Set<MealyState> states = new HashSet<>();
        List<MealyState> q = new ArrayList<>();
        q.add(initialState);
        states.add(initialState);
        while (!q.isEmpty()) {
            MealyState s = q.remove(0);
            for (MealyState next : s.getNextStates()) {
                if (states.add(next)) {
                    q.add(next);
                }
            }
        }

        return states;
    }

    public void writeToDot(String filename) {
        Set<MealyState> states = getStates();
        try (BufferedWriter out = new BufferedWriter(new FileWriter(filename))) {
            out.write("digraph {\nrankdir=LR\n");
            for (MealyState state : states) {
                out.write(String.format("\t%s\n", state.name));
                for (Entry<String, MealyTransition> edge : state.getTransitions()) {
                    out.write(String.format("\t%s -> %s [ label=\"%s/%s\" ]\n", state.name, edge.getValue().to.name,
                            edge.getKey(), edge.getValue().output));
                }
            }
            out.write("}\n");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
