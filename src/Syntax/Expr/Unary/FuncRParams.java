package Syntax.Expr.Unary;

import Lexer.Token;
import Syntax.Expr.Multi.Exp;
import Syntax.Func.FuncDef;

import java.util.ArrayList;

public class FuncRParams {
    private final Exp first;
    private ArrayList<Token> commas;
    private ArrayList<Exp> exps;

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
