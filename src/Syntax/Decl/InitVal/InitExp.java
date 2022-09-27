package Syntax.Decl.InitVal;

import Syntax.Expr.Multi.Exp;

public class InitExp implements InitVal {
    private final boolean isConst;
    private final Exp exp;

    public InitExp(Exp exp, boolean isConst) {
        this.exp = exp;
        this.isConst = isConst;
    }

    @Override
    public String toString() {
        return exp + "\n" + (isConst ? "<ConstInitVal>" : "<InitVal>");
    }
}
