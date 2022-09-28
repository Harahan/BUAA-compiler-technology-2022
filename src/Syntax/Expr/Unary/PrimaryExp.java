package Syntax.Expr.Unary;

import Lexer.Token;
import Syntax.Expr.Multi.Exp;

public class PrimaryExp {
    private LVal lval;
    private Number number;
    private Token lPTk;
    private Token rpTk;
    private Exp exp;

    public PrimaryExp(LVal lval) {
        this.lval = lval;
    }

    public PrimaryExp(Number number) {
        this.number = number;
    }

    public PrimaryExp(Token lPTk) {
        this.lPTk = lPTk;
    }

    public void setExp(Exp exp) {
        assert (number == null && lval == null);
        this.exp = exp;
    }

    public void setRPTk(Token rpTk) {
        assert (number == null && lval == null);
        this.rpTk = rpTk;
    }

    @Override
    public String toString() {
        if (lval != null) return lval + "\n" + "<PrimaryExp>";
        if (number != null) return number + "\n" + "<PrimaryExp>";
        return lPTk + "\n" + exp + "\n" + rpTk + "\n" + "<PrimaryExp>";
    }
}
