package Syntax.Expr.Multi;

public class LAndExp extends MultiExp<EqExp> {
    public LAndExp(EqExp first) {
        super(first, "LAndExp");
    }
}
