package Syntax.Expr.Multi;

import Lexer.Token;
import Syntax.Expr.Unary.UnaryExp;

import java.util.ArrayList;

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

    public T getFirst() {
        return first;
    }

    public ArrayList<T> getTs() {
        return Ts;
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

    public int getFormDim() {
        for (T ts : Ts) {
            if (ts instanceof MultiExp && ((MultiExp<?>) ts).getFormDim() == -10000) return -10000;
            if (ts instanceof UnaryExp && ((UnaryExp) ts).getFormDim() == -10000) return -10000;

        }
        return  (first instanceof MultiExp<?>) ? ((MultiExp<?>) first).getFormDim() : ((UnaryExp) first).getFormDim();
    }
}
