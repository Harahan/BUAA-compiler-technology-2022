package Syntax.Stmt.Simple;


import Lexer.Token;

public class SimpleStmt {
    private Simple spl;
    private Token semicolonTk;

    public SimpleStmt(Simple spl) {
        this.spl = spl;
    }

    public SimpleStmt(Token semicolonTk) {
        this.semicolonTk = semicolonTk;
    }

    public void setSemicolonTk(Token semicolonTk) {
        this.semicolonTk = semicolonTk;
    }

    @Override
    public String toString() {
        return (spl != null ? spl + "\n" : "") + semicolonTk;
    }
}
