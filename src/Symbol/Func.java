package Symbol;


public class Func implements Symbol {
    public enum Type {
        voidFunc, intFunc;
    }

    private final Integer num; // 数量
    private final String name;
    private final Type type;
    private final int loc;
    private Integer addr;
    private final SymbolTable symbolTable;

    public Func(String name, Type type, Integer num, int loc, SymbolTable symbolTable) {
        this.name = name;
        this.type = type;
        this.num = num;
        this.loc = loc;
        this.symbolTable = symbolTable;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLoc() {
        return loc;
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

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    @Override
    public String toString() {
        return "func: " + name + " returnType: " + type + " paramNum: " + num;
    }

    @Override
    public int getDim() {
        return 0;
    }
}
