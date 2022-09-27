package Syntax.Expr.Multi;

public class LOrExp extends MultiExp<LAndExp> {
    public LOrExp(LAndExp first) {
        super(first, "LOrExp");
    }
}
