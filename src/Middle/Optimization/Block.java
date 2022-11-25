package Middle.Optimization;

import Middle.Util.Code;
import Symbol.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

import static Middle.Util.Code.io;

public class Block {
    private final HashSet<Integer> pre = new HashSet<>();
    private final HashSet<Integer> nxt = new HashSet<>();
    private final ArrayList<Code> codes;

    private HashSet<Meta> activeIn = new HashSet<>();
    private HashSet<Meta> activeOut = new HashSet<>();
    private final HashSet<Meta> activeUse = new HashSet<>();
    private final HashSet<Meta> activeDef = new HashSet<>();

    private final HashSet<Meta> arriveGen = new HashSet<>();
    private final HashSet<Meta> arriveKill = new HashSet<>();
    private HashSet<Meta> arriveIn = new HashSet<>();
    private HashSet<Meta> arriveOut = new HashSet<>();

    private final int id;
    private final String func;

    public Block(Integer id, ArrayList<Code> codes, String func) {
        this.codes = codes;
        this.id = id;
        this.func = func;
    }

    public ArrayList<Code> getCodes() {
        return codes;
    }

    public HashSet<Integer> getNxt() {
        return nxt;
    }

    public int getId() {
        return id;
    }

    public HashSet<Integer> getPre() {
        return pre;
    }

    public void calcActiveUseAndDef() {
        HashMap<Symbol, Integer> useMap = new HashMap<Symbol, Integer>();
        HashMap<Symbol, Integer> defMap = new HashMap<Symbol, Integer>();

        // statistic var that first used and first defined
        for (int i = 0; i < codes.size(); ++i) {
            Code code = codes.get(i);
            if (code.getDef() != null && !defMap.containsKey(code.getDef())) defMap.put(code.getDef(), i);
            HashSet<Symbol> codeUse = code.getUse();
            for (Symbol symbol : codeUse) {
                if (!useMap.containsKey(symbol)) useMap.put(symbol, i);
            }
        }

        // calc activeUse and activeDef
        // record the first used and first defined line
        for (int i = 0; i < codes.size(); ++i) {
            Code code = codes.get(i);
            Symbol def = code.getDef();
            if (def != null && defMap.get(def) == i && useMap.getOrDefault(def, Integer.MAX_VALUE) > i) activeDef.add(new Meta(id, i, code, def));
            HashSet<Symbol> uses = code.getUse();
            for (Symbol use : uses) {
                if (useMap.get(use) == i && i <= defMap.getOrDefault(use, Integer.MAX_VALUE)) activeUse.add(new Meta(id, i, code, use));
            }
        }
    }

    public void calcArriveGenAndKill() {
        // calc gen
        for (int i = 0; i < codes.size(); ++i) {
            if (codes.get(i).getGen() != null) arriveGen.add(new Meta(id, i, codes.get(i), codes.get(i).getSymbolRes()));
        }
        // calc kill
        arriveKill.addAll(DataFlow.prepareKill(func, id));
    }

    public ArrayList<Code> deleteDeadCode() {
        ArrayList<Code> newCodes = new ArrayList<>();
        HashSet<Symbol> live = new HashSet<>();
        activeOut.forEach(meta -> live.add(meta.getSymbol()));
        for (int i = codes.size() - 1; i >= 0; --i) {
            Code code = codes.get(i);
            // 注意全局变量的处理
            if ((code.getDef() != null && live.contains(code.getDef())) ||
                    (code.getSymbolRes() != null && code.getSymbolRes().getBlockLevel() == 0) ||
                    code.getDef() == null) {
                live.remove(code.getDef());
                live.addAll(code.getUse());
                newCodes.add(code);
            } else if (code.getDef() != null && !live.contains(code.getDef()) && code.getInstr() == Code.Op.GET_INT) {
                code.clearRes(null);
                newCodes.add(code);
            }
        }
        // reverse newCodes
        ArrayList<Code> res = new ArrayList<>();
        for (int i = newCodes.size() - 1; i >= 0; --i) res.add(newCodes.get(i));
        return res;
    }

    public HashSet<Meta> getArriveGen() {
        return arriveGen;
    }

    public HashSet<Meta> getActiveDef() {
        return activeDef;
    }

    public HashSet<Meta> getActiveIn() {
        return activeIn;
    }

    public HashSet<Meta> getActiveOut() {
        return activeOut;
    }

    public HashSet<Meta> getActiveUse() {
        return activeUse;
    }

    public HashSet<Meta> getArriveIn() {
        return arriveIn;
    }

    public HashSet<Meta> getArriveKill() {
        return arriveKill;
    }

    public HashSet<Meta> getArriveOut() {
        return arriveOut;
    }

    public void setActiveIn(HashSet<Meta> activeIn) {
        this.activeIn = activeIn;
    }

    public void setActiveOut(HashSet<Meta> activeOut) {
        this.activeOut = activeOut;
    }

    public void setArriveIn(HashSet<Meta> arriveIn) {
        this.arriveIn = arriveIn;
    }

    public void setArriveOut(HashSet<Meta> arriveOut) {
        this.arriveOut = arriveOut;
    }

    @Override
    public String toString() {
        return "----------block" + id + " begin----------------\n" +
                "pre: block" + pre + " nxt: block" + nxt + "\n" +
                // for (Code code : codes) sb.append(code).append("\n");
                "arriveIn: " + arriveIn + "\n" +
                "arriveOut: " + arriveOut + "\n" +
                "arriveGen: " + arriveGen + "\n" +
                "arriveKill: " + arriveKill + "\n" +
                "activeIn: " + activeIn + "\n" +
                "activeOut: " + activeOut + "\n" +
                "activeUse: " + activeUse + "\n" +
                "activeDef: " + activeDef + "\n" +
                "----------block" + id + " end----------------\n";
    }

    public static class Meta {
        Integer blockId;
        Integer codeId;
        Code code;
        Symbol symbol;

        public Meta(Integer blockId, Integer codeId, Code code, Symbol res) {
            this.blockId = blockId;
            this.codeId = codeId;
            this.code = code;
            this.symbol = res;
        }

        public Integer getBlockId() {
            return blockId;
        }

        public Integer getCodeId() {
            return codeId;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        public Code getCode() {
            return code;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Meta meta = (Meta) o;
            // it's not necessary to override equals and hashCode for Code, Symbol
            return Objects.equals(blockId, meta.blockId) && Objects.equals(codeId, meta.codeId) && Objects.equals(code, meta.code) && Objects.equals(symbol, meta.symbol);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockId, codeId, code, symbol);
        }

        @Override
        public String toString() {
            return "<" + blockId + ", " + codeId + ", [" + code + "], " + symbol.getNickname() + ">";
        }
    }
}

