package Syntax.Func;

import Lexer.Token;

import java.util.ArrayList;

public class FuncFParams {
    private final FuncFParam first;
    private final ArrayList<FuncFParam> funcFParams = new ArrayList<>();
    private final ArrayList<Token> commas = new ArrayList<>();

    public FuncFParams(FuncFParam first) {
        this.first = first;
    }

    public void addFuncFParam(FuncFParam funcFParam) {
        assert commas.size() - 1 == funcFParams.size();
        funcFParams.add(funcFParam);
    }

    public void addComma(Token comma) {
        assert commas.size() == funcFParams.size();
        commas.add(comma);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(first + "\n");
        for (int i = 0; i < funcFParams.size(); ++i) {
            sb.append(commas.get(i)).append("\n").append(funcFParams.get(i)).append("\n");
        }
        return sb + "<FuncFParams>";
    }
}
