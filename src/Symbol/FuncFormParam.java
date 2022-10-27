package Symbol;

import Middle.Visitor;

import java.util.ArrayList;

public class FuncFormParam implements Symbol {
    private final String name;
    private int addr;
    private final int dim;
    private final ArrayList<Integer> dims = new ArrayList<>();
    private final int blockLevel;
    private final int blockNum;
    private final SymbolTable symbolTable;

    public FuncFormParam(String name, int dim, ArrayList<Integer> dims, SymbolTable symbolTable) {
        this.name = name;
        this.dim = dim;
        if (dims != null)this.dims.addAll(dims);
        this.blockLevel = symbolTable.getBlockLevel();
        this.blockNum = symbolTable.getBlockNum();
        this.symbolTable = symbolTable;
        Visitor.str2Symbol.put(name + "(" + blockLevel + "," + blockNum + ")", this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getBlockLevel() {
        return blockLevel;
    }

    @Override
    public int getBlockNum() {
        return blockNum;
    }

    @Override
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    @Override
    public String getNickname() {
        return name + "(" + blockLevel + "," + blockNum + ")";
    }

    @Override
    public Integer setAndGetAddr(int addr) {
        this.addr = addr;
        return addr + 4;
    }

    @Override
    public Integer getAddr() {
        return addr;
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public Integer getSize() {
        /*int sz = 1;
        for (Integer x : dims) sz *= x;
        return sz * 4;*/
        return 0;
    }

    @Override
    public int getDim() {
        return dim;
    }

    public ArrayList<Integer> getDims() {
        return dims;
    }

    @Override
    public String toString() {
        return "funcParam: " + name + " dim: " + dim;
    }
}
