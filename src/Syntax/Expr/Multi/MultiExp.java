package Syntax.Expr.Multi;

import Lexer.Token;

import java.util.ArrayList;
import java.util.List;

public class MultiExp<T> {
    private final String name;
    private final T first;
    private final ArrayList<Token> operators = new ArrayList<>();
    private final ArrayList<T> Ts = new ArrayList<>();

    public MultiExp(T first, String name) {
        this.name = name;
        this.first = first;
    }

    public void addOperator(Token operator) {
        assert operators.size() == Ts.size();
        operators.add(operator);
    }

    public void addT(T t) {
        assert operators.size() - 1 == Ts.size();
        Ts.add(t);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(first + "\n");
        for (int i = 0; i < Ts.size(); ++i) {
            sb.append("<").append(name).append(">").append("\n").append(operators.get(i)).append("\n")
                    .append(Ts.get(i)).append("\n");
        }
        return sb + "<" + name + ">";
    }
}
