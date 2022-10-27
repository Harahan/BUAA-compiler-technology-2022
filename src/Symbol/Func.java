package Symbol;


import Middle.Visitor;

public class Func implements Symbol {
    public enum Type {
        voidFunc, intFunc;
    }

    private final Integer num; // 参数数量
    private final String name;
    private final Type type;
    private final int blockLevel;
    private final int blockNum;
    private Integer addr;
    private final SymbolTable funcTable;
    private final SymbolTable symbolTable;

    public Func(String name, Type type, Integer num, SymbolTable funcTable, SymbolTable symbolTable) {
        this.name = name;
        this.type = type;
        this.num = num;
        this.blockLevel = symbolTable.getBlockLevel();
        this.funcTable = funcTable;
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
    public Integer setAndGetAddr(int addr) {
        this.addr = addr;
        return addr;
    }

    @Override
    public Integer getSize() {
        return 0;
    }

    @Override
    public Integer getAddr() {
        return addr;
    }

    @Override
    public boolean isConst() {
        return false;
    }

    public Type getType() {
        return type;
    }

    public Integer getNum() {
        return num;
    }

    public SymbolTable getFuncTable() {
        return funcTable;
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
        return "func: " + name + " returnType: " + type + " paramNum: " + num + " <blockLevel, blockNum>: " + blockLevel + ", " + blockNum +
                "\nfuncTable's " + funcTable.toString() + "\n";
    }

    @Override
    public int getDim() {
        return 0;
    }
}
