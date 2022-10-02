package Syntax.Func;

import Lexer.Token;

public class MainFuncDef extends FuncDef {

    public MainFuncDef(Token funcType, Token ident) {
        super(funcType, ident);
    }

    @Override
    public String toString() {
        return funcType + "\n" + ident + "\n" + lPTK + "\n" + rPTK + "\n" + block + "\n" + "<MainFuncDef>";
    }
}
