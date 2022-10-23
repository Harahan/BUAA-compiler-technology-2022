package Syntax.Func;

import Lexer.Token;
import Syntax.Decl.Decl;
import Syntax.Stmt.Stmt;

import java.util.ArrayList;

public class Block {
    private final Token lBTK;
    private Token rBTK;
    private final ArrayList<BlockItem> blockItems = new ArrayList<>();

    public Block(Token lBTK) {
        this.lBTK = lBTK;
    }

    public void setRBTK(Token rBTK) {
        this.rBTK = rBTK;
    }

    public void addBlockItem(BlockItem blockItem) {
        blockItems.add(blockItem);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(lBTK + "\n");
        for (BlockItem blockItem : blockItems) sb.append(blockItem).append("\n");
        sb.append(rBTK).append("\n");
        return sb + "<Block>";
    }

    public ArrayList<BlockItem> getBlockItems() {
        return blockItems;
    }

    public Token getrBTK() {
        return rBTK;
    }

    public static class BlockItem {
        private final Decl decl;
        private final Stmt stmt;

        public BlockItem(Decl decl) {
            this.decl = decl;
            stmt = null;
        }

        public BlockItem(Stmt stmt) {
            this.stmt = stmt;
            decl = null;
        }

        public Stmt getStmt() {
            return stmt;
        }

        public Decl getDecl() {
            return decl;
        }

        @Override
        public String toString() {
            return decl == null ? String.valueOf(stmt) : String.valueOf(decl);
        }
    }

}
