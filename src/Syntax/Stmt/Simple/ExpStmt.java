package Syntax.Stmt.Simple;

import Syntax.Expr.Multi.Exp;

public class ExpStmt implements Simple {
    private final Exp exp;

    public ExpStmt(Exp exp) {
        this.exp = exp;
    }

    public Exp getExp() {
        return exp;
    }

    @Override
    public String toString() {
        return exp.toString();
    }
}
