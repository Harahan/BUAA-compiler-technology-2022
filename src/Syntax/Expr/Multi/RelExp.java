package Syntax.Expr.Multi;

public class RelExp extends MultiExp<AddExp> {
    public RelExp(AddExp first) {
        super(first, "RelExp");
    }
}
