package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

public class VariableList implements Iterable<Variable> {

    private final Variable[] variables;
    private boolean converged;

    public VariableList(Variable[] variables) {
        this.variables = variables;
    }

    public static double[] generateRandomDistribution(int length) {
        double[] d = new Random().doubles(length, 0, 1).toArray();
        double total = Arrays.stream(d).sum();
        return Arrays.stream(d).map(v -> v / total).toArray();
    }

    public void updateAll() {
        converged = true;
        for (Variable variable : variables) {
            converged = variable.update() && converged;
        }
    }

    public boolean hasConverged() {
        return converged;
    }

    public void setVars(VariableList p1, VariableList p2) {
        for (Variable variable : variables) {
            variable.setVars(p1, p2);
        }
    }

    public int length() {
        return variables.length;
    }

    @Override
    public Iterator iterator() {
        return Arrays.stream(variables).iterator();
    }

    public Variable get(int i) {
        return variables[i];
    }

    public String toStringForFile() {
        return "List{" +
                Arrays.toString(variables) +
                ", converged=" + converged +
                '}';
    }
}
