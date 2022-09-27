package Syntax.Expr.Multi;

public class AddExp extends MultiExp<MulExp> {
    public AddExp(MulExp first) {
        super(first, "AddExp");
    }
}
