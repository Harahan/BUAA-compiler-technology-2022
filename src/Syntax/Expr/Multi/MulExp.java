package Syntax.Expr.Multi;

import Syntax.Expr.Unary.UnaryExp;

public class MulExp extends MultiExp<UnaryExp> {
    public MulExp(UnaryExp first) {
        super(first, "MulExp");
    }
}
