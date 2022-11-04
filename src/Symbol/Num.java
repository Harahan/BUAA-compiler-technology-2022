package Symbol;

public class Num implements Symbol {
    private final Integer val;

    public Num(String val) {
        this.val = Integer.valueOf(val);
    }

    public Integer getVal() {
        return val;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public int getBlockLevel() {
        return 100000;
    }

    @Override
    public int getBlockNum() {
        return 100000;
    }

    @Override
    public Integer setAndGetAddr(int addr) {
        return null;
    }

    @Override
    public Integer getSize() {
        return null;
    }

    @Override
    public Integer getAddr() {
        return null;
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
        return null;
    }

    @Override
    public String getNickname() {
        return String.valueOf(val);
    }
}
