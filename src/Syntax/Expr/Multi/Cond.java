package Syntax.Expr.Multi;

public class Cond {
    private final LOrExp lOrExp;

    public Cond(LOrExp lOrExp) {
        this.lOrExp = lOrExp;
    }

    @Override
    public String toString() {
        return lOrExp + "\n" + "<Cond>";
    }
}
