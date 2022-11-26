package Middle.Optimization;

import Backend.Util.RegAlloc;
import Middle.Util.Code;
import Symbol.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

import static Middle.Util.Code.io;

public class Block {
    private final HashSet<Integer> pre = new HashSet<>();
    private final HashSet<Integer> nxt = new HashSet<>();
    private ArrayList<Code> codes;

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
        // clear
        activeUse.clear();
        activeDef.clear();
        activeIn.clear();
        activeOut.clear();
        HashMap<Symbol, Integer> useMap = new HashMap<Symbol, Integer>();
        HashMap<Symbol, Integer> defMap = new HashMap<Symbol, Integer>();

        // statistic var that first used and first defined
        for (int i = 0; i < codes.size(); ++i) {
            Code code = codes.get(i);
            if (code.getDef() != null && !defMap.containsKey(code.getDef())) defMap.put(code.getDef(), i);
            HashSet<Symbol> codeUse = new HashSet<Symbol>(code.getUse().keySet());
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
            HashSet<Symbol> uses = new HashSet<Symbol>(code.getUse().keySet());
            for (Symbol use : uses) {
                if (useMap.get(use) == i && i <= defMap.getOrDefault(use, Integer.MAX_VALUE)) activeUse.add(new Meta(id, i, code, use));
            }
        }
    }

    public void calcArriveGenAndKill() {
        // clear
        arriveGen.clear();
        arriveKill.clear();
        arriveIn.clear();
        arriveOut.clear();
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
        // System.out.println(live);
        for (int i = codes.size() - 1; i >= 0; --i) {
            Code code = codes.get(i);
            // 注意全局变量的处理
            if ((code.getDef() != null && live.contains(code.getDef())) ||
                    (code.getSymbolRes() != null && code.getSymbolRes().getBlockLevel() == 0) ||
                    code.getDef() == null) {
                live.remove(code.getDef());
                live.addAll(new HashSet<Symbol>(code.getUse().keySet()));
                newCodes.add(code);
            } else if (code.getDef() != null && !live.contains(code.getDef()) && code.getInstr() == Code.Op.GET_INT) {
                code.clearRes(null);
                newCodes.add(code);
            }
        }
        // reverse newCodes
        ArrayList<Code> res = new ArrayList<>();
        for (int i = newCodes.size() - 1; i >= 0; --i) res.add(newCodes.get(i));
        // System.out.println(res);
        codes = res;
        return res;
    }

    public boolean broadcastCode() {
        boolean changed = false;
        ArrayList<Code> defList = new ArrayList<>();
        for (Code code : codes) {
            for (Symbol symbol : code.getUse().keySet()) {
                Code p = null;
                int i = defList.size() - 1;
                boolean flag = false;
                for (; i >= 0; --i) {
                    p = defList.get(i);
                    if (p.getDef() != null && p.getDef().equals(symbol)) {
                        flag = true;
                        break;
                    }
                }
                if (flag) {
                    // broadcast const
                    if ((p.getInstr() == Code.Op.ASSIGN  || p.getInstr() == Code.Op.DEF_VAL) && p.getSymbolOrd1() == null
                            && !Objects.equals(p.getOrd1(), "(RT)") && !Objects.equals(p.getOrd1(), "(EMPTY)")) {
                        code.reSet(code.getUse().get(symbol), p.getOrd1(), null);
                        changed = true;
                    }
                    // broadcast var
                    else if ((p.getInstr() == Code.Op.ASSIGN || p.getInstr() == Code.Op.DEF_VAL) && p.getSymbolOrd1() != null
                            && p.getSymbolOrd1().getBlockLevel() != 0 && p.getSymbolOrd1().getDim() == 0) {
                        for (int j = i + 1; j < defList.size(); ++j) {
                            Code q = defList.get(j);
                            if (q.getDef() != null && q.getDef().equals(p.getSymbolOrd1())) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag) {
                            code.reSet(code.getUse().get(symbol), p.getOrd1(), p.getSymbolOrd1());
                            changed = true;
                        }
                    }
                }

                if (!flag && arriveIn.stream().filter(meta -> meta.getSymbol().equals(symbol)).count() == 1) {
                    // in arriveIn and only one
                    // only broadcast const
                    // System.out.println(activeIn);
                    Meta meta = arriveIn.stream().filter(meta1 -> meta1.getSymbol().equals(symbol)).collect(Collectors.toList()).get(0);
                    p = meta.getCode();
                    // 不会出现不赋值直接使用的情况
                    if ((p.getInstr() == Code.Op.ASSIGN || p.getInstr() == Code.Op.DEF_VAL) && p.getSymbolOrd1() == null
                            && !Objects.equals(p.getOrd1(), "(RT)") && !Objects.equals(p.getOrd1(), "(EMPTY)")) {
                        code.reSet(code.getUse().get(symbol), p.getOrd1(), null);
                        changed = true;
                    }
                }
            }
            Symbol def = code.getDef();
            if ("-0123456789".indexOf(code.getOrd1().charAt(0)) != -1 && "-0123456789".indexOf(code.getOrd2().charAt(0)) != -1) {
                changed = true;
                int res = 0;
                switch (code.getInstr()) {
                    case ADD:
                        res = Integer.parseInt(code.getOrd1()) + Integer.parseInt(code.getOrd2());
                        break;
                    case SUB:
                        res = Integer.parseInt(code.getOrd1()) - Integer.parseInt(code.getOrd2());
                        break;
                    case MUL:
                        res = Integer.parseInt(code.getOrd1()) * Integer.parseInt(code.getOrd2());
                        break;
                    case DIV:
                        res = Integer.parseInt(code.getOrd1()) / Integer.parseInt(code.getOrd2());
                        break;
                    case MOD:
                        res = Integer.parseInt(code.getOrd1()) % Integer.parseInt(code.getOrd2());
                        break;
                    case EQ:
                        res = Integer.parseInt(code.getOrd1()) == Integer.parseInt(code.getOrd2()) ? 1 : 0;
                        break;
                    case NE:
                        res = Integer.parseInt(code.getOrd1()) != Integer.parseInt(code.getOrd2()) ? 1 : 0;
                        break;
                    case LT:
                        res = Integer.parseInt(code.getOrd1()) < Integer.parseInt(code.getOrd2()) ? 1 : 0;
                        break;
                    case LE:
                        res = Integer.parseInt(code.getOrd1()) <= Integer.parseInt(code.getOrd2()) ? 1 : 0;
                        break;
                    case GT:
                        res = Integer.parseInt(code.getOrd1()) > Integer.parseInt(code.getOrd2()) ? 1 : 0;
                        break;
                    case GE:
                        res = Integer.parseInt(code.getOrd1()) >= Integer.parseInt(code.getOrd2()) ? 1 : 0;
                        break;
                    default:
                        break;
                }
                code.clearOrd1(String.valueOf(res));
                code.clearOrd2(null);
                code.setOp(Code.Op.ASSIGN);
            }
            if (def != null) defList.add(code);
        }
        return changed;
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
                "codes: " + codes + "\n" +
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

