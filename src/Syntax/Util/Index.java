package Syntax.Util;

import Lexer.Token;
import Syntax.Expr.Multi.Exp;

public class Index {
    private Token lBTk;
    private Token rBTk;
    private Exp exp;

    public Index(Token lBTk) {
        this.lBTk = lBTk;
    }

    public void setExp(Exp Exp) {
        this.exp = Exp;
    }

    public void setRBTk(Token rBTk) {
        this.rBTk = rBTk;
    }

    @Override
    public String toString() {
        return lBTk + (exp == null ? "" : ("\n" + exp)) + "\n" + rBTk;
    }
}