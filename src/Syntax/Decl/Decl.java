package Syntax.Decl;

import Lexer.Token;
import Lexer.Type;

import java.util.ArrayList;

public class Decl {
    private final Token constTk;
    private final Token bTypeTk;
    private Def first;
    private final ArrayList<Def> defs = new ArrayList<>();
    private final ArrayList<Token> commas = new ArrayList<>();
    private Token semicolonTk = null;
    // VarDecl -> BType VarDef { ',' VarDef } ';'
    // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'

    public Decl(Token constTk, Token bTypeTk) {
        assert (constTk == null || constTk.getType() == Type.CONSTTK);
        assert bTypeTk.getType() == Type.INTTK;
        this.constTk = constTk;
        this.bTypeTk = bTypeTk;
    }

    public void addDef(Def def) {
        if (first == null) first = def;
        else defs.add(def);
    }

    public void addComma(Token commaTk) {
        assert commaTk.getType() == Type.COMMA;
        commas.add(commaTk);
    }

    public void setSemicolonTk(Token semicolonTk) {
        assert (this.semicolonTk == null && semicolonTk.getType() == Type.SEMICN);
        this.semicolonTk = semicolonTk;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (constTk != null) sb.append(constTk).append("\n");
        sb.append(bTypeTk).append("\n").append(first).append("\n");
        for (int i = 0; i < defs.size(); ++i) {
            sb.append(commas.get(i)).append("\n").append(defs.get(i)).append("\n");
        }
        sb.append(semicolonTk).append("\n");
        return sb + (constTk == null ? "<VarDef>" : "<ConstDef>");
    }
}
