package Syntax.Func;

import Lexer.Token;

public class MainFuncDef extends FuncDef {

    public MainFuncDef(Token funcType, Token ident) {
        super(funcType, ident);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(funcType + "\n" + ident + "\n" + lPTK + "\n");
        sb.append(rPTK).append("\n").append(block).append("\n");
        return sb + "<MainFuncDef>";
    }
}
