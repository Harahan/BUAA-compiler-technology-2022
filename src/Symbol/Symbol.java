package Symbol;

public interface Symbol {
    String getName();
    int getBlockLevel();
    int getBlockNum();
    Integer setAndGetAddr(int addr); // 设置首地址并返回接下来的地址
    Integer getSize(); // 实际变量大小
    Integer getAddr(); // 首地址

    boolean isConst();

    int getDim();

    SymbolTable getSymbolTable();
    String getNickname();
}
