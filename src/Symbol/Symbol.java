package Symbol;

public interface Symbol {
    String getName();
    int getLoc();
    Integer setAndGetAddr(int addr); // 设置首地址并返回接下来的地址
    Integer getSize(); // 实际变量大小
    Integer getAddr(); // 首地址

    boolean isConst();

    int getDim();
}
