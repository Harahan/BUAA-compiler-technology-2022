package Symbol;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {
    private final HashMap<String, Symbol> symbols = new HashMap<>();
    private final ArrayList<Symbol> otherSymbols = new ArrayList<>();
    private final SymbolTable fa;
    private int size = 0;


    public SymbolTable(SymbolTable fa) {
        this.fa = fa;
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
        otherSymbols.add(symbol);
        size += symbol.getSize();
    }

    public ArrayList<Symbol> getOtherSymbols() {
        return otherSymbols;
    }

    public SymbolTable getFa() {
        return fa;
    }
}
