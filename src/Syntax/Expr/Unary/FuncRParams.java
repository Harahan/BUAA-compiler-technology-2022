package Syntax.Expr.Unary;

import Lexer.Token;
import Syntax.Expr.Multi.Exp;

import java.util.ArrayList;

public class FuncRParams {
    private final Exp first;
    private final ArrayList<Token> commas = new ArrayList<>();
    private final ArrayList<Exp> exps = new ArrayList<>();

    public FuncRParams(Exp first) {
        this.first = first;
    }

    public void addExp(Exp exp) {
        assert commas.size() - 1 == exps.size();
        exps.add(exp);
    }

    public void addComma(Token commaTk) {
        assert commas.size() == exps.size();
        commas.add(commaTk);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(first + "\n");
        for (int i = 0; i < exps.size(); ++i) {
            sb.append(commas.get(i)).append("\n").append(exps.get(i)).append("\n");
        }
        return sb + "<FuncRParams>";
    }
}
