package Syntax.Decl.InitVal;

import Lexer.Token;

import java.util.ArrayList;

public class InitArr implements InitVal {
    private final Token lBTk;
    private final boolean isConst;
    private Token rBTK;
    private InitVal first;
    private final ArrayList<InitVal> vars = new ArrayList<>();
    private final ArrayList<Token> commas = new ArrayList<>();

    public InitArr(Token lBTk, boolean isConst) {
        this.lBTk = lBTk;
        this.isConst = isConst;
    }

    public void addVar(InitVal initVal) {
        if (first == null) first = initVal;
        else vars.add(initVal);
    }

    public void addComma(Token comma) {
        assert commas.size() == vars.size();
        commas.add(comma);
    }

    public void setRBTK(Token rBTK) {
        this.rBTK = rBTK;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(lBTk + "\n");
        if (first != null) sb.append(first).append("\n");
        for (int i = 0; i < vars.size(); ++i) {
            sb.append(commas.get(i)).append("\n").append(vars.get(i)).append("\n");
        }
        sb.append(rBTK).append("\n");
        return sb + (isConst ? "<ConstInitVal>" : "<InitVal>");
    }

    public InitVal getFirst() {
        return first;
    }

    public ArrayList<InitVal> getVars() {
        return new ArrayList<InitVal>() {{if (first != null) {add(first); addAll(vars);}}};
    }

    @Override
    public boolean isConst() {
        return isConst;
    }
}
