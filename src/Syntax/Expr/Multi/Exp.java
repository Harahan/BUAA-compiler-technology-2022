package Syntax.Expr.Multi;

public class Exp {
    protected final AddExp addExp;

    public Exp(AddExp addExp) {
        this.addExp = addExp;
    }

    public AddExp getAddExp() {
        return addExp;
    }

    public int getFormDim() {
        return addExp.getFormDim();
    }

    @Override
    public String toString() {
        return addExp + "\n" + "<Exp>";
    }
}
