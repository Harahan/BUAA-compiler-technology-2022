package Syntax.Stmt.Simple;

import Lexer.Token;
import Syntax.Expr.Multi.Exp;

public class ReturnStmt implements Simple {
    private final Token retTK;
    private Exp exp;

    public ReturnStmt(Token retTK) {
        this.retTK = retTK;
    }

    public void setExp(Exp exp) {
        this.exp = exp;
    }

    @Override
    public String toString() {
        return retTK + ((exp != null) ? "\n" + exp : "");
    }
}
