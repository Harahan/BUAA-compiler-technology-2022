package Syntax.Expr.Unary;

import Lexer.Token;
import Middle.Visitor;
import Symbol.SymbolTable;
import Syntax.Util.Index;

import java.util.ArrayList;

public class LVal {
    private final Token identTk;
    private final ArrayList<Index> indexes = new ArrayList<>();

    public LVal(Token identTk) {
        this.identTk = identTk;
    }

    public void addIndex(Index index) {
        assert indexes.size() <= 1;
        indexes.add(index);
    }

    public Token getIdentTk() {
        return identTk;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(identTk + "\n");
        indexes.forEach(idx -> sb.append(idx).append("\n"));
        return sb + "<LVal>";
    }

    public ArrayList<Index> getIndexes() {
        return indexes;
    }

    public int getFormDim() {
        SymbolTable cur = Visitor.curTable;
        if (cur.contains(identTk.getStrVal(), true)) {
            return cur.get(identTk.getStrVal(), true).getDim() - indexes.size();
        }
        return -10000;
    }
}
