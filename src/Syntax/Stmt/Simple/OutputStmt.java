package Syntax.Stmt.Simple;

import Lexer.Token;
import Syntax.Expr.Multi.Exp;

import java.util.ArrayList;

public class OutputStmt implements Simple {
    private final Token printTK;
    private Token lPTK;
    private Token rPTK;
    private Token sTK;
    private final ArrayList<Token> commas = new ArrayList<>();
    private final ArrayList<Exp> exps = new ArrayList<>();

    public OutputStmt(Token printTK) {
        this.printTK = printTK;
    }

    public void setLPTK(Token lPTK) {
        this.lPTK = lPTK;
    }

    public void setRPTK(Token rPTK) {
        this.rPTK = rPTK;
    }

    public void setSTK(Token sTK) {
        this.sTK = sTK;
    }

    public void addComma(Token comma) {
        assert commas.size() == exps.size();
        commas.add(comma);
    }

    public void addExp(Exp exp) {
        assert exps.size() == commas.size() - 1;
        exps.add(exp);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(printTK + "\n" + lPTK + "\n" + sTK + "\n");
        for (int i = 0; i < exps.size(); ++i) {
            sb.append(commas.get(i)).append("\n").append(exps.get(i)).append("\n");
        }
        return sb + rPTK.toString();
    }
}
