package Syntax.Stmt;

import Syntax.Stmt.Simple.SimpleStmt;

public class Stmt {
    private SimpleStmt spl;
    private IfStmt ifStmt;
    private WhileStmt whileStmt;


    public Stmt(SimpleStmt spl) {
        this.spl = spl;
    }

    public Stmt(IfStmt ifStmt) {
        this.ifStmt = ifStmt;
    }

    public Stmt(WhileStmt whileStmt) {
        this.whileStmt = whileStmt;
    }

    @Override
    public String toString() {
        return (spl != null ? spl :
                ifStmt != null ? ifStmt : whileStmt) + "\n" + "<Stmt>";
    }
}
