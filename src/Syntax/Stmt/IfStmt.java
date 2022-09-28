package Syntax.Stmt;

import Lexer.Token;
import Syntax.Expr.Multi.Cond;

public class IfStmt {
    private final Token ifTK;
    private Token lPTK;
    private Token rPTK;
    private Cond cond;
    private Stmt ifStmt;
    private Token elseTK;
    private Stmt elseStmt;

    public IfStmt(Token ifTK) {
        this.ifTK = ifTK;
    }

    public void setLPTK(Token lPTK) {
        this.lPTK = lPTK;
    }

    public void setRPTK(Token rPTK) {
        this.rPTK = rPTK;
    }

    public void setCond(Cond cond) {
        this.cond = cond;
    }

    public void setIfStmt(Stmt ifStmt) {
        this.ifStmt = ifStmt;
    }

    public void setElseStmt(Stmt elseStmt) {
        this.elseStmt = elseStmt;
    }

    public void setElseTK(Token elseTK) {
        this.elseTK = elseTK;
    }

    @Override
    public String toString() {
        return ifTK + "\n" + lPTK + "\n" + cond + "\n" + rPTK + "\n" + ifStmt +
                (elseTK != null ? "\n" + elseTK + "\n" + elseStmt : "");
    }
}
