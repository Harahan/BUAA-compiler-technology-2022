package Syntax.Func;

import Lexer.Token;

public class FuncDef {
    protected final Token funcType;
    protected final Token ident;
    protected Token lPTK;
    protected Token rPTK;
    protected FuncFParams funcFParams;
    protected Block block;

    public FuncDef(Token funcType, Token ident) {
        this.funcType = funcType;
        this.ident = ident;
    }

    public void setLPTK(Token lPTK) {
        this.lPTK = lPTK;
    }

    public void setRPTK(Token rPTK) {
        this.rPTK = rPTK;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public void setFuncFParams(FuncFParams funcFParams) {
        this.funcFParams = funcFParams;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(funcType + "\n" + ident + "\n" + lPTK + "\n");
        if (funcFParams != null) sb.append(funcFParams).append("\n");
        sb.append(rPTK).append("\n").append(block).append("\n");
        return sb + "<FuncDef>";
    }
}
