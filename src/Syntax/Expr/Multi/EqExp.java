package Syntax.Expr.Multi;

public class EqExp extends MultiExp<RelExp> {
    public EqExp(RelExp first) {
        super(first, "EqExp");
    }
}
