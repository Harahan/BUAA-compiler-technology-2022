package Syntax.Decl;

import Lexer.Token;
import Lexer.Type;
import Syntax.Decl.InitVal.InitVal;
import Syntax.Util.Index;

import java.util.ArrayList;

public class Def {
    private final boolean isConst;
    private final Token identTk;
    private final ArrayList<Index> indexes = new ArrayList<>();
    private Token assignTk;
    private InitVal initVal;

    public Def(boolean isConst, Token identTk) {
        this.isConst = isConst;
        this.identTk = identTk;
    }

    public void addIndex(Index index) {
        assert indexes.size() <= 1;
        indexes.add(index);
    }

    public void setAssignTk(Token assignTk) {
        assert assignTk.getType() == Type.ASSIGN;
        this.assignTk = assignTk;
    }

    public void setInitVal(InitVal initVal) {
        this.initVal = initVal;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(identTk + "\n");
        for (Index index : indexes) sb.append(index).append("\n");
        if (assignTk != null) sb.append(assignTk).append("\n").append(initVal).append("\n");
        return sb + (isConst ? "<ConstDef>" : "<VarDef>");
    }
}
