package Middle.Util;

import Lexer.Token;
import Middle.Visitor;
import Symbol.Symbol;
import Syntax.Expr.Multi.Exp;
import Syntax.Expr.Multi.MultiExp;
import Syntax.Expr.Unary.LVal;
import Syntax.Expr.Unary.Number;
import Syntax.Expr.Unary.PrimaryExp;
import Syntax.Expr.Unary.UnaryExp;
import Symbol.Val;
import Syntax.Util.Index;

import java.util.ArrayList;

public class Calc {
    public static Integer calcExp(Exp Exp) throws Exception {
        return calcMultiExp(Exp.getAddExp());
    }

    private static Integer calcMultiExp(MultiExp<?> multiExp) throws Exception {
        Object first = multiExp.getFirst();
        Integer res = calcMultiOrUnaryExp(first);
        for (int i = 0; i < multiExp.getTs().size(); ++i) {
            Token op = multiExp.getOperators().get(i);
            Object o = multiExp.getTs().get(i);
            Integer x = calcMultiOrUnaryExp(o);
            // x == 0 表示有报错

            switch (op.getType()) {
                case MINU: res -= x; break;
                case PLUS: res += x; break;
                case MOD: res %= x; break;
                case MULT: res *= x; break;
                case DIV: res /= x; break;
                case AND: res &= x; break;
                default: assert false;
            }
        }
        return res;
    }

    private static Integer calcMultiOrUnaryExp(Object o) throws Exception {
        return o instanceof MultiExp<?> ? calcMultiExp((MultiExp<?>) o) : calcUnaryExp((UnaryExp) o);
    }

    private static Integer calcUnaryExp(UnaryExp unaryExp) throws Exception {
        Token op = unaryExp.getUnaryOp();
        PrimaryExp primaryExp = unaryExp.getPrimaryExp();
        int res = 0;

        if (op != null) {

            switch (op.getType()) {
                case PLUS: res = calcUnaryExp(unaryExp.getUnaryExp()); break;
                case MINU: res = -calcUnaryExp(unaryExp.getUnaryExp()); break;
                case NOT: res = calcUnaryExp(unaryExp.getUnaryExp()) == 0 ? 1 : 0; break;
                default: assert false;
            }

        } else if (primaryExp != null) {

            Number number = primaryExp.getNumber();
            LVal lVal = primaryExp.getLval();
            Exp exp = primaryExp.getExp();
            if (number != null) res = number.val();
            else if (lVal != null) res = calcLVal(lVal);
            else res = calcExp(exp);

        } else {
            throw new Exception();
        }
        return res;
    }

    public static Integer calcLVal(LVal lVal) throws Exception {
        Token ident = lVal.getIdentTk();

        boolean x = Visitor.curTable.contains(ident.getStrVal(), true);
        // 随便乱找的数
        if (!x) throw new Exception();
        Symbol sym = Visitor.curTable.get(ident.getStrVal(), true);
        if (!sym.isConst() && !(Visitor.curTable.getBlockLevel() == 0)) throw new Exception();
        ArrayList<Integer> arr = new ArrayList<>();
        for (Index index : lVal.getIndexes()) arr.add(calcExp(index.getExp()));
        return ((Val) sym).getVal(arr);
    }
}
