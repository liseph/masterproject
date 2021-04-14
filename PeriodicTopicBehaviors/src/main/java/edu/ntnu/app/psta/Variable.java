package edu.ntnu.app.psta;

public interface Variable {
    boolean update();
    void setVars(VariableList p1, VariableList p2);
    double get(int... values);
}
