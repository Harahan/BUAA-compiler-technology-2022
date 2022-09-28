package Syntax.Stmt;

import Lexer.Token;
import Syntax.Expr.Multi.Cond;

public class WhileStmt {
    private final Token whileTK;
    private Token lPTK;
    private Token rPTK;
    private Cond cond;
    private Stmt stmt;

    public WhileStmt(Token whileTK) {
        this.whileTK = whileTK;
    }

    public void setCond(Cond cond) {
        this.cond = cond;
    }

    public void setLPTK(Token lPTK) {
        this.lPTK = lPTK;
    }

    public void setRPTK(Token rPTK) {
        this.rPTK = rPTK;
    }

    public void setStmt(Stmt stmt) {
        this.stmt = stmt;
    }

    @Override
    public String toString() {
        return whileTK + "\n" + lPTK + "\n" + cond + "\n" + rPTK + "\n" + stmt;
    }
}
