package Syntax.Stmt.Simple;

import Lexer.Token;

public class LoopStmt implements Simple {
    private final Token contrTK;

    public LoopStmt(Token contrTK) {
        this.contrTK = contrTK;
    }

    @Override
    public String toString() {
        return contrTK.toString();
    }
}
