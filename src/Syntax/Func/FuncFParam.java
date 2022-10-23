package Syntax.Func;

import Lexer.Token;
import Syntax.Util.Index;

import java.util.ArrayList;

public class FuncFParam {
    private final Token bType;
    private final Token ident;
    private final ArrayList<Index> indexes = new ArrayList<>();

    public FuncFParam(Token bType, Token ident) {
        this.bType = bType;
        this.ident = ident;
    }

    public void addIndex(Index index) {
        assert indexes.size() <= 1;
        indexes.add(index);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(bType + "\n" + ident + "\n");
        for (Index index : indexes) {
            sb.append(index).append("\n");
        }
        return sb + "<FuncFParam>";
    }

    public Token getIdent() {
        return ident;
    }

    public boolean isArr() {
        return indexes.size() != 0;
    }

    public int getDim() {
        return indexes.size();
    }

    public ArrayList<Index> getIndexes() {
        return indexes;
    }
}
