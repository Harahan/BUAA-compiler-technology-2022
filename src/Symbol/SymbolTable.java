package Symbol;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {
    private final HashMap<String, Symbol> symbols = new HashMap<>();
    private final ArrayList<Symbol> orderSymbols = new ArrayList<>();
    private final SymbolTable fa;
    private final ArrayList<SymbolTable> sons = new ArrayList<>();
    private int size = 0;
    public int totSize = 0;
    private final int blockLevel;
    private final int blockNum;

    public final boolean isFunc;


    public SymbolTable(SymbolTable fa, int blockLevel, int blockNum, boolean isFunc) {
        this.fa = fa;
        this.blockLevel = blockLevel;
        this.blockNum = blockNum;
        this.isFunc = isFunc;
    }

    public boolean contains(String name, boolean rec) {
        if (symbols.containsKey(name)) return true;
        if (fa != null && rec) return fa.contains(name, true);
        return false;
    }

    public Symbol get(String name, boolean rec) {
        if (symbols.containsKey(name)) return symbols.get(name);
        if (fa != null && rec) return fa.get(name, true);
        return null;
    }

    public void add(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
        orderSymbols.add(symbol);
        size += symbol.getSize();
    }

    public void add(SymbolTable symbolTable) {
        sons.add(symbolTable);
    }

    public ArrayList<SymbolTable> getSons() {
        return sons;
    }

    public int getBlockLevel() {
        return blockLevel;
    }

    public int getBlockNum() {
        return blockNum;
    }

    public ArrayList<Symbol> getOrderSymbols() {
        return orderSymbols;
    }

    public SymbolTable getFa() {
        return fa;
    }

    public String getNickName() {
        return "(" + blockLevel + "," + blockNum + ")";
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return  "<blockLevel, blockNum>: " + blockLevel + ", " + blockNum;
    }
}
