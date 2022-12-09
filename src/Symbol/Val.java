package Symbol;

import Middle.Visitor;

import java.util.ArrayList;

public class Val implements Symbol {
    private final String name;
    private final boolean isConst;
    private final ArrayList<Integer> initVal = new ArrayList<>();
    private int addr;
    private final int dim;
    private final ArrayList<Integer> dims = new ArrayList<>();
    private final int blockLevel;
    private final int blockNum;
    private final SymbolTable symbolTable;
    private boolean hasInitVal = true;

    public boolean isArrayInMain = false;

    public Val(String name, boolean isConst, int dim, ArrayList<Integer> dims, SymbolTable symbolTable) {
        this.name = name;
        this.isConst = isConst;
        this.dim = dim;
        if (dims != null) this.dims.addAll(dims);
        this.blockLevel = symbolTable.getBlockLevel();
        this.blockNum = symbolTable.getBlockNum();
        this.symbolTable = symbolTable;
        Visitor.str2Symbol.put(name + "(" + blockLevel + "," + blockNum + ")", this);
    }

    public void addInitVal(Integer initVal) {
        if (initVal == null) hasInitVal = false;
        this.initVal.add(initVal);
        //System.out.println(name + ": " + this.initVal);
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isConst() {
        return isConst;
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
        return addr + getSize();
    }

    @Override
    public Integer getSize() {
        int sz = 1;
        for (Integer x : dims) sz *= x;
        return sz * 4;
    }

    @Override
    public int getDim() {
        return dim;
    }

    public ArrayList<Integer> getDims() {
        return dims;
    }

    public Integer getAddr() {
        return addr;
    }

    @Override
    public String toString() {
        return "val: " + name + " isConst: " + isConst + " dim: " + dim;
    }

    public int getVal(ArrayList<Integer> arr) throws Exception {
        if (arr.size() != dim || arr.size() > 2) throw new Exception();

        if (arr.size() == 0) return initVal.get(0);

        int idx = (arr.size() == 1 ? arr.get(0) : (arr.get(0) * dims.get(1) + arr.get(1)));

        return initVal.get(idx);
    }

    @Override
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    @Override
    public String getNickname() {
        String x = "";
        //if (dim == 1) x += "[" + dims.get(0) + "]";
        //if (dim == 2) x += "[" + dims.get(0) + "," + dims.get(1) + "]";
        return name + x + "(" + blockLevel + "," + blockNum + ")";
    }

    public boolean isHasInitVal() {
        return hasInitVal && initVal.size() != 0;
    }

    public ArrayList<Integer> getInitVal() {
        return initVal;
    }
}
