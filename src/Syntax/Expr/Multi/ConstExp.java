package Syntax.Expr.Multi;

public class ConstExp extends Exp {
    public ConstExp(AddExp addExp) {
        super(addExp);
    }

    @Override
    public String toString() {
        return super.addExp + "\n" + "<ConstExp>";
    }
}
