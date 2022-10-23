package Syntax.Stmt;

import Syntax.Func.Block;

public class BlockStmt {
    private final Block block;

    public BlockStmt(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return block.toString();
    }
}
