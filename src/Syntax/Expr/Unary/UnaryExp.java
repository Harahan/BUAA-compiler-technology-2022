package Syntax.Expr.Unary;

import Lexer.Token;
import Lexer.Type;
import Middle.Visitor;
import Symbol.Symbol;
import Symbol.Func;

public class UnaryExp {
    private PrimaryExp primaryExp;
    private Token identTk;
    private Token lPTk;
    private FuncRParams funcRParams;
    private Token rPTk;
    private Token unaryOp;
    private UnaryExp unaryExp;

    public UnaryExp(PrimaryExp primaryExp) {
        this.primaryExp = primaryExp;
    }

    public UnaryExp(Token token) {
        if (token.getType() == Type.IDENFR) this.identTk = token;
        else this.unaryOp = token;
    }

    public void setLPTk(Token lPTk) {
        this.lPTk = lPTk;
    }

    public void setFuncRParams(FuncRParams funcRParams) {
        this.funcRParams = funcRParams;
    }

    public void setRPTk(Token rPTk) {
        this.rPTk = rPTk;
    }

    public void setUnaryExp(UnaryExp unaryExp) {
        this.unaryExp = unaryExp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (primaryExp != null) sb.append(primaryExp).append("\n");
        else if (identTk != null) sb.append(identTk).append("\n").append(lPTk.toString()).append("\n").append(funcRParams != null ? funcRParams + "\n" : "").append(rPTk).append("\n");
        else sb.append(unaryOp).append("\n<UnaryOp>\n").append(unaryExp).append("\n");
        return sb + "<UnaryExp>";
    }

    public PrimaryExp getPrimaryExp() {
        return primaryExp;
    }

    public Token getIdentTk() {
        return identTk;
    }

    public UnaryExp getUnaryExp() {
        return unaryExp;
    }

    public FuncRParams getFuncRParams() {
        return funcRParams;
    }

    public Token getUnaryOp() {
        return unaryOp;
    }

    public int getFormDim() {
        if (primaryExp != null) return primaryExp.getFormDim();
        else if (unaryOp != null) return unaryExp.getFormDim();
        else if (!Visitor.curTable.contains(identTk.getStrVal(), true)) {
            return -10000;
        } else {
            Symbol symbol = Visitor.curTable.get(identTk.getStrVal(), true);
            assert symbol instanceof Func;
            if (((Func) symbol).getType() == Func.Type.intFunc) return 0;
            else return -1; // TODO
            /*
            void a() {}
            int fun() {return 1;}
            int main() {fun(a());}
             */
        }
    }
}
