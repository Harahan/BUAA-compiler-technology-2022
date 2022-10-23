package Syntax.Stmt.Simple;

import Lexer.Token;
import Syntax.Expr.Multi.Exp;
import Syntax.Expr.Unary.LVal;

public class AssignStmt implements Simple {
    private final LVal lVal;
    private Token assignTK;
    private Exp exp;

    public AssignStmt(LVal lVal) {
        this.lVal = lVal;
    }

    public void setAssignTK(Token assignTK) {
        this.assignTK = assignTK;
    }

    public void setExp(Exp exp) {
        this.exp = exp;
    }

    public Exp getExp() {
        return exp;
    }

    public LVal getlVal() {
        return lVal;
    }

    @Override
    public String toString() {
        return lVal + "\n" + assignTK + "\n" + exp;
    }
}
