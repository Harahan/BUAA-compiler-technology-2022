package Syntax.Stmt;

import Syntax.Func.Block;

public class BlockStmt {
    private final Block block;

    public BlockStmt(Block block) {
        this.block = block;
    }

    @Override
    public String toString() {
        return block.toString();
    }
}
