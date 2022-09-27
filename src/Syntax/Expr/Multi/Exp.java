package Syntax.Expr.Multi;

public class Exp {
    protected final AddExp addExp;

    public Exp(AddExp addExp) {
        this.addExp = addExp;
    }

    @Override
    public String toString() {
        return addExp + "\n" + "<Exp>";
    }
}
