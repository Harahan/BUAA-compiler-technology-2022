package Syntax.Expr.Unary;

import Lexer.Token;
import Lexer.Type;

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
        else if (identTk != null) sb.append(identTk).append("\n").append(lPTk.toString()).append("\n").append(funcRParams).append("\n").append(rPTk).append("\n");
        else sb.append(unaryOp).append("\n<UnaryOp>\n").append(unaryExp).append("\n");
        return sb + "<UnaryExp>";
    }
}
