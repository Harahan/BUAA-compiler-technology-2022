package Syntax.Stmt;

import Syntax.Stmt.Simple.SimpleStmt;

public class Stmt {
    private SimpleStmt spl;
    private IfStmt ifStmt;
    private WhileStmt whileStmt;

    private BlockStmt blockStmt;


    public Stmt(SimpleStmt spl) {
        this.spl = spl;
    }

    public Stmt(IfStmt ifStmt) {
        this.ifStmt = ifStmt;
    }

    public Stmt(BlockStmt blockStmt) {
        this.blockStmt = blockStmt;
    }

    public Stmt(WhileStmt whileStmt) {
        this.whileStmt = whileStmt;
    }

    @Override
    public String toString() {
        return (spl != null ? spl :
                ifStmt != null ? ifStmt :
                        blockStmt != null ? blockStmt : whileStmt) + "\n" + "<Stmt>";
    }
}
