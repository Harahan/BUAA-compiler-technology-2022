package Middle.Optimization;

import Backend.MipsGenerator;
import Middle.Util.Code;
import Symbol.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

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

    public final boolean hasCall;

    private final int id;
    private final String func;

    public int lp = 0;

    public Block(Integer id, ArrayList<Code> codes, String func) {
        this.codes = codes;
        this.id = id;
        this.func = func;
        this.hasCall = codes.stream().anyMatch(x -> x.getInstr() == Code.Op.CALL);
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
            if (def != null && defMap.get(def) == i && useMap.getOrDefault(def, Integer.MAX_VALUE) > i) activeDef.add(new Meta(-1, -1, null, def));
            HashSet<Symbol> uses = new HashSet<Symbol>(code.getUse().keySet());
            for (Symbol use : uses) {
                if (useMap.get(use) == i && i <= defMap.getOrDefault(use, Integer.MAX_VALUE)) activeUse.add(new Meta(-1, -1, null, use));
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
        for (int i = codes.size() - 1; i >= 0; --i) {
            int finalI = i;
            if (codes.get(i).getGen() != null && arriveGen.stream().noneMatch(meta -> meta.getSymbol().equals(codes.get(finalI).getGen()))) arriveGen.add(new Meta(id, i, codes.get(i), codes.get(i).getSymbolRes()));
            if (codes.get(i).getGen() != null) {
                for (int j = codes.size() - 1; j >= 0; --j) {
                    if (j == i) continue;
                    if (codes.get(j).getGen() == codes.get(i).getGen()) arriveKill.add(new Meta(id, j, codes.get(j), codes.get(j).getSymbolRes()));
                }
            }
        }
        // calc kill
        arriveKill.addAll(DataFlow.prepareKill(func, id));
    }

    public ArrayList<Code> deleteDeadCode() {
        int sz = codes.size();
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
        boolean flag = false;
        // System.out.println(res);
        if (MipsGenerator.optimize.get("MidCodeOptimize")) {
            for (int i = 0; i < res.size(); ++i) {
                Code code = res.get(i);
                // if (code.getRes().equals("(LABEL2)")) System.out.println("====" + res);
                if (code.getInstr() == Code.Op.EQZ_JUMP && i - 1 >= 0 && res.get(i - 1).getInstr() == Code.Op.ASSIGN
                        && "-0123456789".indexOf(res.get(i - 1).getOrd1().charAt(0)) != -1
                        && res.get(i - 1).getRes().equals(code.getOrd1())) {
                    flag = true;
                    int x = Integer.parseInt(res.get(i - 1).getOrd1());
                    // res.remove(--i);
                    if (x == 0) {
                        code.setOp(Code.Op.JUMP);
                        code.clearOrd1(code.getRes());
                        code.clearOrd2(null);
                        code.clearRes(null);
                        // System.out.println("optimize: " + code);
                        --i;
                    } else {
                        res.remove(i--);
                    }
                } else if (code.getInstr() == Code.Op.NEZ_JUMP && i - 1 >= 0 && res.get(i - 1).getInstr() == Code.Op.ASSIGN
                        && "-0123456789".indexOf(res.get(i - 1).getOrd1().charAt(0)) != -1
                        && res.get(i - 1).getRes().equals(code.getOrd1())) {
                    flag = true;
                    int x = Integer.parseInt(res.get(i - 1).getOrd1());
                    // res.remove(--i);
                    if (x != 0) {
                        code.setOp(Code.Op.JUMP);
                        code.clearOrd1(code.getRes());
                        code.clearOrd2(null);
                        code.clearRes(null);
                        // System.out.println(code);
                        --i;
                    } else {
                        res.remove(i--);
                    }
                } else if (code.getInstr() == Code.Op.EQZ_JUMP && "-0123456789".indexOf(code.getOrd1().charAt(0)) != -1) {
                    flag = true;
                    int x = Integer.parseInt(code.getOrd1());
                    if (x == 0) {
                        code.setOp(Code.Op.JUMP);
                        code.clearOrd1(code.getRes());
                        code.clearOrd2(null);
                        code.clearRes(null);
                        // System.out.println(code);
                        --i;
                    } else {
                        res.remove(i--);
                    }
                } else if (code.getInstr() == Code.Op.NEZ_JUMP && "-0123456789".indexOf(code.getOrd1().charAt(0)) != -1) {
                    //System.out.println(code);
                    flag = true;
                    int x = Integer.parseInt(code.getOrd1());
                    if (x != 0) {
                        code.setOp(Code.Op.JUMP);
                        code.clearOrd1(code.getRes());
                        code.clearOrd2(null);
                        code.clearRes(null);
                        // System.out.println(code);
                        --i;
                    } else {
                        res.remove(i--);
                    }
                } else if (code.getInstr() == Code.Op.JUMP && i + 1 < res.size() && res.get(i + 1).getInstr() == Code.Op.JUMP) {
                    flag = true;
                    res.remove(i + 1);
                } else if (Code.alu.contains(code.getInstr()) && code.getInstr() != Code.Op.ASSIGN /*&& i + 1 < res.size() &&
                        (code.getRes().equals(res.get(i + 1).getOrd1()) || code.getRes().equals(res.get(i + 1).getOrd2()))*/) {
                    Code.Op op = code.getInstr();
                    String ord1 = code.getOrd1();
                    String ord2 = code.getOrd2();
                    // Code nxtCode = res.get(i + 1);
                    String t = null;
                    boolean tag = false;
                    // int x = code.getRes().equals(res.get(i + 1).getOrd1()) ? 1 : 2;
                    if (op == Code.Op.MUL && (ord1.equals("1") || ord2.equals("1"))) {
                        t = ord1.equals("1") ? ord2 : ord1;
                        code.setOp(Code.Op.ASSIGN);
                        if (ord1.equals("1")) {
                            code.clearOrd1(ord2);
                            code.clearOrd2(null);
                        } else code.clearOrd2(null);
                        tag = true;
                    } else if (op == Code.Op.MUL && (ord1.equals("0") || ord2.equals("0"))) {
                        t = "0";
                        code.setOp(Code.Op.ASSIGN);
                        code.clearOrd1("0");
                        code.clearOrd2(null);
                        tag = true;
                    } else if (op == Code.Op.ADD && (ord1.equals("0") || ord2.equals("0"))) {
                        t = ord1.equals("0") ? ord2 : ord1;
                        // System.out.println(code);
                        code.setOp(Code.Op.ASSIGN);
                        if (ord1.equals("0")) {
                            code.clearOrd1(ord2);
                            code.clearOrd2(null);
                        } else code.clearOrd2(null);
                        tag = true;
                    } else if (op == Code.Op.SUB && ord2.equals("0")) {
                        t = ord1;
                        code.setOp(Code.Op.ASSIGN);
                        code.clearOrd2(null);
                        tag = true;
                    } else if (op == Code.Op.DIV && ord2.equals("1")) {
                        t = ord1;
                        code.setOp(Code.Op.ASSIGN);
                        code.clearOrd2(null);
                        tag = true;
                    }
                    if (tag && i + 1 < res.size() &&
                            (code.getRes().equals(res.get(i + 1).getOrd1()) || code.getRes().equals(res.get(i + 1).getOrd2()))) {
                        Code nxtCode = res.get(i + 1);
                        int x = code.getRes().equals(res.get(i + 1).getOrd1()) ? 1 : 2;
                        if (x == 1 && !nxtCode.getOrd1().equals(t)) {
                            //flag = true;
                            nxtCode.clearOrd1(t);
                        } else if (!nxtCode.getOrd2().equals(t)) {
                            //flag = true;
                            nxtCode.clearOrd2(t);
                        }
                    }
                    if (tag) flag = true;
                }
            }
        }

        // peep hole optimization
        if (MipsGenerator.optimize.get("MidCodeOptimize")) {
            for (int i = 0; i < res.size(); ++i) {
                Code code = res.get(i);
                Code nxt = i + 1 < res.size() ? res.get(i + 1) : null;
                if (code.getOrd1().equals(code.getRes())) continue;
                if (nxt != null && check(code, Code.Op.ADD) && check(nxt, Code.Op.SUB) && code.getRes().equals(nxt.getOrd1())) {
                    int x = Integer.parseInt(code.getOrd2());
                    int y = Integer.parseInt(nxt.getOrd2());
                    nxt.clearOrd1(code.getOrd1());
                    nxt.clearOrd2(String.valueOf(y - x));
                    flag = true;
                } else if (nxt != null && check(code, Code.Op.SUB) && check(nxt, Code.Op.ADD) && code.getRes().equals(nxt.getOrd1())) {
                    int x = Integer.parseInt(code.getOrd2());
                    int y = Integer.parseInt(nxt.getOrd2());
                    nxt.clearOrd1(code.getOrd1());
                    nxt.clearOrd2(String.valueOf(y - x));
                    flag = true;
                } else if (nxt != null && check(code, Code.Op.SUB) && check(nxt, Code.Op.SUB) && code.getRes().equals(nxt.getOrd1())) {
                    int x = Integer.parseInt(code.getOrd2());
                    int y = Integer.parseInt(nxt.getOrd2());
                    nxt.clearOrd1(code.getOrd1());
                    nxt.clearOrd2(String.valueOf(x + y));
                    flag = true;
                } else if (nxt != null && check(code, Code.Op.ADD) && check(nxt, Code.Op.ADD) && code.getRes().equals(nxt.getOrd1())) {
                    int x = Integer.parseInt(code.getOrd2());
                    int y = Integer.parseInt(nxt.getOrd2());
                    nxt.clearOrd1(code.getOrd1());
                    nxt.clearOrd2(String.valueOf(x + y));
                    flag = true;
                } else if (nxt != null && check(code, Code.Op.MUL) && check(nxt, Code.Op.MUL) && code.getRes().equals(nxt.getOrd1())) {
                    int x = Integer.parseInt(code.getOrd2());
                    int y = Integer.parseInt(nxt.getOrd2());
                    nxt.clearOrd1(code.getOrd1());
                    nxt.clearOrd2(String.valueOf(x * y));
                    flag = true;
                }
            }
        }

        if (flag && res.size() == sz) {
            //System.out.println(codes);
            res.add(new Code(Code.Op.NOP, "(EMPTY)", "(EMPTY)", "(EMPTY)"));
            // System.out.println(res);
        }
        codes = res;
        //System.out.println("Block " + id + " " + codes);
        return res;
    }


    boolean check(Code code, Code.Op op) {
        return code.getInstr() == op && "-0123456789".indexOf(code.getOrd2().charAt(0)) != -1;
    }

    public boolean broadcastCode() {
        boolean changed = false;
        ArrayList<Code> defList = new ArrayList<>();
        for (Code code : codes) {
            for (Symbol symbol : code.getUse().keySet()) {
                if (symbol.getBlockLevel() == 0) continue;
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
                        // System.out.println(code);
                        changed = true;
                    }
                    // broadcast var
                    else if ((p.getInstr() == Code.Op.ASSIGN || p.getInstr() == Code.Op.DEF_VAL) && p.getSymbolOrd1() != null
                            && p.getSymbolOrd1().getBlockLevel() != 0 && p.getSymbolOrd1().getDim() == 0 && !symbol.equals(p.getSymbolOrd1())) {
                        boolean tag = true;
                        for (int j = i + 1; j < defList.size(); ++j) {
                            Code q = defList.get(j);
                            if (q.getDef() != null && q.getDef().equals(p.getSymbolOrd1())) {
                                tag = false;
                                break;
                            }
                        }
                        if (tag) {
                            code.reSet(code.getUse().get(symbol), p.getOrd1(), p.getSymbolOrd1());
                            // System.out.println(code);
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
            if (MipsGenerator.optimize.get("MidCodeOptimize")) {
                if ("-0123456789".indexOf(code.getOrd1().charAt(0)) != -1 && "-0123456789".indexOf(code.getOrd2().charAt(0)) != -1) {
                    // System.out.println(code);
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
                    // System.out.println(code);
                }
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

    public void setCodes(ArrayList<Code> codes) {
        this.codes = codes;
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
            if (code == null) return symbol.getNickname();
            return "<" + blockId + ", " + codeId + ", [" + code + "], " + symbol.getNickname() + ">";
        }
    }

    // 计算block中每一条代码的到达in和out
    public HashMap<Integer, HashSet<Meta>> calcCodeArriveIn() {
        HashMap<Integer, HashSet<Meta>> codeArriveIn = new HashMap<>();
        HashMap<Integer, HashSet<Meta>> codeArriveOut = new HashMap<>();
        for (int i = 0; i < codes.size(); i++) {
            Code code = codes.get(i);
            HashSet<Meta> in = new HashSet<>();
            if (i == 0) {
                in.addAll(arriveIn);
            } else {
                in.addAll(codeArriveOut.get(i - 1));
            }
            HashSet<Meta> out = new HashSet<>(in);
            Symbol res = code.getGen();
            if (res != null) {
                out.removeIf(meta -> meta.getSymbol().equals(res));
                out.add(new Meta(id, i, code, res));
            }
            codeArriveIn.put(i, in);
            codeArriveOut.put(i, out);
        }
        return codeArriveIn;
    }

    // 计算block中每一条代码的活跃in和out
    public HashMap<Integer, HashSet<Meta>> calcCodeActiveOut() {
        HashMap<Integer, HashSet<Meta>> codeActiveIn = new HashMap<>();
        HashMap<Integer, HashSet<Meta>> codeActiveOut = new HashMap<>();
        for (int i = codes.size() - 1; i >= 0; i--) {
            Code code = codes.get(i);
            HashSet<Meta> out = new HashSet<>();
            if (i == codes.size() - 1) {
                out.addAll(activeOut);
            } else {
                out.addAll(codeActiveIn.get(i + 1));
            }
            HashSet<Meta> in = new HashSet<>(out);
            Symbol res = code.getDef();
            if (res != null) {
                in.removeIf(meta -> meta.getSymbol().equals(res));
            }
            in.addAll(code.getUse().keySet().stream().map(symbol -> new Meta(-1, -1, null, symbol)).collect(Collectors.toSet()));
            codeActiveIn.put(i, in);
            codeActiveOut.put(i, out);
        }
        return codeActiveOut;
    }

}

