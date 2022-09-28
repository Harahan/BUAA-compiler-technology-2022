package Syntax.Stmt.Simple;

import Lexer.Token;
import Syntax.Expr.Unary.LVal;

public class InputStmt implements Simple {
    private final LVal lVal;
    private Token assignTK;
    private Token getintTK;
    private Token lPTK;
    private Token rPTK;

    public InputStmt(LVal lVal) {
        this.lVal = lVal;
    }

    public void setAssignTK(Token assignTK) {
        this.assignTK = assignTK;
    }

    public void setGetintTK(Token getintTK) {
        this.getintTK = getintTK;
    }

    public void setLPTK(Token lPTK) {
        this.lPTK = lPTK;
    }

    public void setRPTK(Token rPTK) {
        this.rPTK = rPTK;
    }

    @Override
    public String toString() {
        return lVal + "\n" + assignTK + "\n" + getintTK + "\n" + lPTK + "\n" + rPTK;
    }
}
