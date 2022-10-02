package Syntax.Expr.Unary;

import Lexer.Token;

public class Number {
    private final Token intConstTk;

    public Number(Token intConstTk) {
        this.intConstTk = intConstTk;
    }

    @Override
    public String toString() {
        return intConstTk + "\n" + "<Number>";
    }
}
