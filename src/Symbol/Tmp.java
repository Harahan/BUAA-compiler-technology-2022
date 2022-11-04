package Symbol;

import Middle.Visitor;

public class Tmp implements Symbol {
    private String name;
    private int addr;
    private final int blockLevel;
    private final int blockNum;
    private final SymbolTable symbolTable;

    public Tmp(String name, SymbolTable symbolTable) {
        this.name = name;
        this.symbolTable = symbolTable;
        this.blockLevel = symbolTable.getBlockLevel();
        this.blockNum = symbolTable.getBlockNum();
        Visitor.str2Symbol.put(name, this);
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
    public Integer setAndGetAddr(int addr) {
        this.addr = addr;
        return addr + 4;
    }

    @Override
    public Integer getSize() {
        return 4;
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
    public int getDim() {
        return 0;
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
    public String toString() {
        return getNickname();
    }
}
