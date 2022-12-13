package Backend;

import Backend.Optimization.MulDiv;
import Backend.Optimization.PeepHole;
import Backend.Util.ColorAlloc;
import Backend.Util.Instruction;
import Backend.Util.RegAlloc;
import Middle.MidCodeList;
import Middle.Optimization.DataFlow;
import Middle.Util.Code;
import Middle.Visitor;
import Symbol.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MipsGenerator {
    public static HashMap<String, Boolean> optimize = new HashMap<String, Boolean>() {{
        put("MulDiv", true);
        put("DeleteDeadCode", true);
        put("PeepHole", true);
        put("BroadcastCode", true); // const and val both
        put("CountVal", true);
        put("MidCodeOptimize", true);
        put("JumpOptimize", true);
        put("RemoveRedundantCall", true);
        put("ExtractLoopConstExp", true);
        put("ArrayInMainToGlobal", true);
        put("GlobalPositionOptimize", false); // may be a negative effect
    }};

    public static final Pattern valPattern = Pattern.compile(".*\\[(.*)]");
    public static ArrayList<String> mipsCodeList = new ArrayList<>();
    public static HashMap<Symbol, Integer> val2Used = new HashMap<>();
    private final ArrayList<ArrayList<Code>> funcList = new ArrayList<>();
    private final HashMap<Symbol, Integer> globalArrAddr = new HashMap<>();

    public static final Pattern tempPattern = Pattern.compile(".*\\(T(\\d+)\\).*");
    private ArrayList<Code> mainList = new ArrayList<>();

    public static RegAlloc ra;

    public MipsGenerator(ArrayList<Code> codes) {
        // System.out.println(codes);

        if (optimize.get("CountVal")) {
            for (Code code : codes) {
                Symbol o1 = hasTmp(code.getOrd1());
                Symbol o2 = hasTmp(code.getOrd2());
                Symbol r = hasTmp(code.getRes());
                if (o1 != null) val2Used.merge(o1, 1, Integer::sum);
                if (o2 != null) val2Used.merge(o2, 1, Integer::sum);
                if (r != null) val2Used.merge(r, 1, Integer::sum);
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (codes.get(i).getInstr() == Code.Op.FUNC) {
                String name = codes.get(i).getOrd1();
                ArrayList<Code> func = new ArrayList<>();
                while (codes.get(i).getInstr() != Code.Op.FUNC_END) func.add(codes.get(i++));
                func.add(codes.get(i));
                if (name.startsWith("main")) mainList = func;
                else funcList.add(func);
            }
        }
        setOff(Visitor.global, 0);
        translate();
        if (optimize.get("PeepHole")) {
            PeepHole.peepHole(mipsCodeList);
            PeepHole.peepHole(mipsCodeList);
        }
    }

    // ------------------------------------- PREPARE ------------------------------------------------------------

    public static String getName(Symbol sym) {
        return sym.getName() + "_" + sym.getBlockLevel() + "_" + sym.getBlockNum();
    }

    public static Symbol hasTmp(String var) {
        Matcher matcher = tempPattern.matcher(var);
        if (matcher.matches()) {
            return Visitor.str2Symbol.get("(T" + matcher.group(1) + ")");
        }
        return null;
    }

    // off --> val: 0
    // off --> arr: 数组内偏移量 * 4
    // off为数字
    // gp: - sp: +
    public static void pushBackOrLoadFromMem(String reg, Symbol sym, Integer off, Instruction.LS.Op op) {
        if (optimize.get("CountVal")) {
            if (val2Used.containsKey(sym) && val2Used.get(sym) == 0 && op == Instruction.LS.Op.sw && !ra.inGraph(sym)) {
                ra.refreshOne(reg);
                return;
            }
        }
        if (sym instanceof Tmp)
            mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, off + sym.getAddr(), "$sp")));
        if (sym instanceof Val) {
            Val val = (Val) sym;
            if (val.getBlockLevel() != 0 && !val.isArrayInMain) {
                mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, off + sym.getAddr(), "$sp")));
            } else {
                if (val.getDim() == 0 && !optimize.get("GlobalPositionOptimize"))
                    mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, -sym.getAddr(), "$gp")));
                else mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, getName(sym), off, "$zero")));
            }
        }
        if (sym instanceof FuncFormParam && sym.getDim() > 0) {
            String addReg = ra.find(sym);
            if (addReg == null) {
                addReg = ra.alloc(sym, true, reg, true);
                mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.lw, addReg, null, sym.getAddr(), "$sp")));
            }
            mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, off, addReg)));
        } else if (sym instanceof FuncFormParam) {
            mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, off + sym.getAddr(), "$sp")));
        }
    }

    // -----------------------------------------------
    // reg <---> mem(sym + off(T))
    // reg <---> mem(sym + off(int))
    // int ---> mem(sym + off(T))
    // int ---> mem(sym + off(int))

    // off --> val: 0
    // off --> arr: 数组内偏移量 * 4
    // off为临时变量
    // gp: - sp: +
    public static void pushBackOrLoadFromMem(String reg, Symbol sym, Symbol off, Instruction.LS.Op op) {
        String tmpReg = ra.find(off);
        if (tmpReg == null) {
            tmpReg = ra.alloc(off, true, reg, true);
            pushBackOrLoadFromMem(tmpReg, off, 0, Instruction.LS.Op.lw);
        }
        mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sll, "$a1", tmpReg, 2)));
        tmpReg = "$a1";
        if (sym instanceof Val) {
            Val val = (Val) sym;
            if (val.getBlockLevel() != 0 && !val.isArrayInMain) {
                mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, tmpReg, tmpReg, sym.getAddr())));
                mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, tmpReg, tmpReg, "$sp")));
                mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, 0, tmpReg)));
            } else {
                mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, getName(sym), 0, tmpReg)));
            }
        }
        if (sym instanceof FuncFormParam) {
            String addReg = ra.find(sym);
            if (addReg == null) {
                addReg = ra.alloc(sym, true, reg, true);
                mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.lw, addReg, null, sym.getAddr(), "$sp")));
            }
            mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, tmpReg, tmpReg, addReg)));
            mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, 0, tmpReg)));
        }
    }

    public static void pushBackMem(Integer imm, Symbol sym, Integer off) {
        if (imm != 0) {
            mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$a0", imm)));
            pushBackOrLoadFromMem("$a0", sym, off, Instruction.LS.Op.sw);
        } else {
            pushBackOrLoadFromMem("$zero", sym, off, Instruction.LS.Op.sw);
        }
    }

    /**
     * @param rVal： reg, number, Val, funcFormParam
     * @param lVal: Val, funcFormParam, tmp，对于funcFormParam数组直接回写数组内存具体索引地址
     */
    public static void saveLVal(String rVal, String lVal) {
        // lVal
        Matcher m = Code.indexPattern.matcher(lVal);
        Matcher n = Code.varPattern.matcher(lVal);
        Symbol symbolLVal = null;
        if (n.matches()) symbolLVal = Visitor.str2Symbol.get(n.group(1));
        else {
            n = Code.tempVarPattern.matcher(lVal);
            if (n.matches()) symbolLVal = Visitor.str2Symbol.get(n.group(0));
        }
        Integer immOffLVal = null;
        Symbol symOffLVal = null;
        if (m.matches()) {
            try {
                immOffLVal = Integer.valueOf(m.group(1));
            } catch (Exception e) {
                symOffLVal = Visitor.str2Symbol.get(m.group(1));
            }
        }

        //  reg --> lVal
        if (rVal.startsWith("$")) {
            assert symbolLVal != null;
            if (symbolLVal.getDim() == 0) {
                String resReg = ra.find(symbolLVal);
                if (resReg == null) resReg = ra.alloc(symbolLVal, false, null, true);
                if (resReg != null && !resReg.equals(rVal)) {
                    mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, resReg, rVal)));
                    ra.setDirty(resReg);
                } else pushBackOrLoadFromMem(rVal, symbolLVal, 0, Instruction.LS.Op.sw);
            } else if (immOffLVal != null)
                pushBackOrLoadFromMem(rVal, symbolLVal, immOffLVal * 4, Instruction.LS.Op.sw);
            else if (symOffLVal != null) pushBackOrLoadFromMem(rVal, symbolLVal, symOffLVal, Instruction.LS.Op.sw);
            return;
        }

        // number --> lVal
        if ("-0123456789".indexOf(rVal.charAt(0)) != -1) {
            if (immOffLVal != null) pushBackMem(Integer.valueOf(rVal), symbolLVal, immOffLVal * 4);
            else if (symOffLVal != null) pushBackMem(Integer.valueOf(rVal), symbolLVal, symOffLVal);
            else {
                String reg = ra.find(symbolLVal);
                if (reg == null) reg = ra.alloc(symbolLVal, false, null, true);
                if (reg == null) pushBackMem(Integer.valueOf(rVal), symbolLVal, 0);
                else {
                    mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, reg, Integer.valueOf(rVal))));
                    ra.setDirty(reg);
                }
            }
            return;
        }

        // Val --> lVal
        m = Code.indexPattern.matcher(rVal);
        n = Code.varPattern.matcher(rVal);
        Symbol symbolRVal = null;
        if (n.matches()) symbolRVal = Visitor.str2Symbol.get(n.group(1));
        else {
            n = Code.tempVarPattern.matcher(rVal);
            if (n.matches()) symbolRVal = Visitor.str2Symbol.get(n.group(0));
        }
        Integer immOffRVal = null;
        Symbol symOffRVal = null;
        if (m.matches()) {
            try {
                immOffRVal = Integer.valueOf(m.group(1));
            } catch (Exception e) {
                symOffRVal = Visitor.str2Symbol.get(m.group(1));
            }
        }

        assert symbolRVal != null;
        // rVal: 0
        // rVal: 1~2,此时LVal必是0维,且马上会用到
        if (symbolRVal.getDim() == 0) {
            // rVal: 零维变量
            String src = ra.find(symbolRVal);
            if (src == null) {
                src = ra.alloc(symbolRVal, true, null, true);
                pushBackOrLoadFromMem(src, symbolRVal, 0, Instruction.LS.Op.lw);
            }
            if (symOffLVal == null && immOffLVal == null) {
                // lVal: 0 rVal: 0
                String reg = ra.find(symbolLVal);
                if (reg == null) reg = ra.alloc(symbolLVal, false, null, true);
                if (reg != null && !reg.equals(src)) {
                    mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, reg, src)));
                    ra.setDirty(reg);
                } else pushBackOrLoadFromMem(src, symbolLVal, 0, Instruction.LS.Op.sw);
            } else if (symOffLVal != null) pushBackOrLoadFromMem(src, symbolLVal, symOffLVal, Instruction.LS.Op.sw);
            else pushBackOrLoadFromMem(src, symbolLVal, immOffLVal * 4, Instruction.LS.Op.sw);

        } else {
            String src = ra.find(symbolLVal);
            if (src == null) src = ra.alloc(symbolLVal, true, null, true);
            ra.setDirty(src);
            if (symOffRVal != null) pushBackOrLoadFromMem(src, symbolRVal, symOffRVal, Instruction.LS.Op.lw);
            else if (immOffRVal != null) pushBackOrLoadFromMem(src, symbolRVal, immOffRVal * 4, Instruction.LS.Op.lw);
        }
    }

    public static void pushBackMem(Integer imm, Symbol sym, Symbol off) {
        if (imm != 0) {
            mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$a0", imm)));
            pushBackOrLoadFromMem("$a0", sym, off, Instruction.LS.Op.sw);
        } else {
            pushBackOrLoadFromMem("$zero", sym, off, Instruction.LS.Op.sw);
        }
    }

    public int setOff(SymbolTable nd, int off) {
        // System.out.println(nd);
        int init = off;
        for (int i = 0; i < nd.getOrderSymbols().size(); ++i) {
            Symbol sym = nd.getOrderSymbols().get(i);
            if (sym instanceof Val && ((Val) sym).isArrayInMain) continue;
            // System.out.println(sym.getName() + " ---> " + off);
            off = sym.setAndGetAddr(off);
        }
        for (int i = 0; i < nd.getSons().size(); ++i) {
            SymbolTable st = nd.getSons().get(i);
            off += setOff(st, st.isFunc ? 0 : off);
        }
        nd.totSize = off - init;
        return nd.isFunc ? 0 : nd.totSize;
    }

    /**
     * @param reg:  reg
     * @param lVal: number, val, funcFormParam, reg
     *              tips: 如果lVal在寄存器中或者为寄存器，不会对reg建立和lVal(或者lVal中存储变量)的映射, 对于funFormParam的数组直接从内存具体索引加载值
     */
    public void loadLVal(String reg, String lVal) {
        // number --> reg
        if ("-0123456789".indexOf(lVal.charAt(0)) != -1) {
            mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, reg, Integer.valueOf(lVal))));
            return;
        }

        // reg --> reg
        if (lVal.startsWith("$")) {
            mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, reg, lVal)));
            return;
        }

        // Val --> reg
        Matcher m = Code.indexPattern.matcher(lVal);
        Matcher n = Code.varPattern.matcher(lVal);
        Symbol symbolLVal = null;
        if (n.matches()) symbolLVal = Visitor.str2Symbol.get(n.group(1));
        else {
            n = Code.tempVarPattern.matcher(lVal);
            if (n.matches()) symbolLVal = Visitor.str2Symbol.get(n.group(0));
        }
        Integer immOffLVal = null;
        Symbol symOffLVal = null;
        if (m.matches()) {
            try {
                immOffLVal = Integer.valueOf(m.group(1));
            } catch (Exception e) {
                // System.out.println(m.group(1));
                symOffLVal = Visitor.str2Symbol.get(m.group(1));
            }
        }

        String regLVal = ra.find(symbolLVal);
        if (regLVal != null && !reg.equals(regLVal))
            mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, reg, regLVal)));
        else if (symOffLVal == null && immOffLVal == null)
            pushBackOrLoadFromMem(reg, symbolLVal, 0, Instruction.LS.Op.lw);
        else if (symOffLVal != null) pushBackOrLoadFromMem(reg, symbolLVal, symOffLVal, Instruction.LS.Op.lw);
        else pushBackOrLoadFromMem(reg, symbolLVal, immOffLVal * 4, Instruction.LS.Op.lw);
    }

    public void printInt(String lVal) {
        loadLVal("$a0", lVal);
        mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$v0", 1)));
        mipsCodeList.add(String.valueOf(new Instruction.NP(Instruction.NP.Op.syscall)));
    }

    public void printStr(String label) {
        mipsCodeList.add(String.valueOf(new Instruction.ML(Instruction.ML.Op.la, "$a0", label.substring(1, label.length() - 1))));
        mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$v0", 4)));
        mipsCodeList.add(String.valueOf(new Instruction.NP(Instruction.NP.Op.syscall)));
    }

    public void getInt(String res) {
        mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$v0", 5)));
        mipsCodeList.add(String.valueOf(new Instruction.NP(Instruction.NP.Op.syscall)));
        if (!res.equals("(EMPTY)")) saveLVal("$v0", res);
    }

    /**
     * 不会有数组变量，至少要3个寄存器
     */
    public void alu(Code.Op op, String res, String ord1, String ord2, Symbol symbolRes, Symbol symbolOrd1, Symbol symbolOrd2) {
        String regRes;
        regRes = ra.find(symbolRes);
        if (regRes == null) regRes = ra.alloc(symbolRes, true, null, false);

        if (symbolOrd1 == null && symbolOrd2 == null && op != Code.Op.NOT) {
            // 数字+数字
            Integer intOrd1 = Integer.valueOf(ord1), intOrd2 = Integer.valueOf(ord2);
            Integer ans = 0;
            switch (op) {
                case ADD:
                    ans = intOrd1 + intOrd2;
                    break;
                case SUB:
                    ans = intOrd1 - intOrd2;
                    break;
                case MUL:
                    ans = intOrd1 * intOrd2;
                    break;
                case DIV:
                    ans = intOrd1 / intOrd2;
                    break;
                case MOD:
                    ans = intOrd1 % intOrd2;
                    break;
                case EQ:
                    ans = (intOrd1.equals(intOrd2) ? 1 : 0);
                    break;
                case NE:
                    ans = (!intOrd1.equals(intOrd2) ? 1 : 0);
                    break;
                case GT:
                    ans = (intOrd1 > intOrd2 ? 1 : 0);
                    break;
                case GE:
                    ans = (intOrd1 >= intOrd2 ? 1 : 0);
                    break;
                case LT:
                    ans = (intOrd1 < intOrd2 ? 1 : 0);
                    break;
                case LE:
                    ans = (intOrd1 <= intOrd2 ? 1 : 0);
                    break;
            }
            mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, regRes, ans)));
        } else {
            // mul optimize
            if (op == Code.Op.MUL && optimize.get("MulDiv")) {
                boolean tag = false;
                Symbol symbolVal = null;
                String val = null;
                Integer num = null;
                if (symbolOrd1 == null) {
                    num = Integer.valueOf(ord1);
                    symbolVal = symbolOrd2;
                    val = ord2;
                    tag = true;
                } else if (symbolOrd2 == null) {
                    num = Integer.valueOf(ord2);
                    symbolVal = symbolOrd1;
                    val = ord1;
                    tag = true;
                }
                if (tag) {
                    if (num == 0) {
                        mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, regRes, 0)));
                    } else {
                        String x = ra.find(symbolVal);
                        if (x == null && !symbolVal.equals(symbolRes)) {
                            x = ra.alloc(symbolVal, true, regRes, true);
                            pushBackOrLoadFromMem(x, symbolVal, 0, Instruction.LS.Op.lw);
                        } else if (x == null) {
                            x = regRes;
                            ra.map(x, symbolVal);
                            pushBackOrLoadFromMem(x, symbolVal, 0, Instruction.LS.Op.lw);
                        }
                        if (num == -1) {
                            mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.neg, regRes, x)));
                            ra.map(regRes, symbolRes);
                            ra.setDirty(regRes);
                            return;
                        }
                        if (x.equals(regRes) && Integer.bitCount(num) != 1 && !(Integer.bitCount(num) == 2 && num % 2 == 1)) {
                            mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, "$v0", x)));
                            x = "$v0";
                        }
                        mipsCodeList.addAll(MulDiv.mul(regRes, x, num, 4));
                    }
                    ra.map(regRes, symbolRes);
                    ra.setDirty(regRes);
                    return;
                }
            }
            // div optimize
            if ((op == Code.Op.DIV || op == Code.Op.MOD) && symbolOrd2 == null && optimize.get("MulDiv")) {
                int d = Integer.parseInt(ord2);
                String x = ra.find(symbolOrd1);
                if (x == null && !symbolOrd1.equals(symbolRes)) {
                    x = ra.alloc(symbolOrd1, true, regRes, true);
                    pushBackOrLoadFromMem(x, symbolOrd1, 0, Instruction.LS.Op.lw);
                } else if (x == null) {
                    x = regRes;
                    ra.map(x, symbolOrd1);
                    pushBackOrLoadFromMem(x, symbolOrd1, 0, Instruction.LS.Op.lw);
                }
                if (x.equals(regRes)) {
                    mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, "$v0", x)));
                    x = "$v0";
                }
                mipsCodeList.addAll(MulDiv.div(regRes, x, d, 50, op == Code.Op.DIV, symbolOrd1));
                ra.map(regRes, symbolRes);
                ra.setDirty(regRes);
                return;
            }
            // 零维变量 @ 零维变量
            String regOrd1 = null, regOrd2 = null;
            int tag = 0;
            // ord1
            if (symbolOrd1 != null) {
                regOrd1 = ra.find(symbolOrd1);
                if (regOrd1 == null && !symbolOrd1.equals(symbolRes)) {
                    regOrd1 = ra.alloc(symbolOrd1, true, null, true);
                    pushBackOrLoadFromMem(regOrd1, symbolOrd1, 0, Instruction.LS.Op.lw);
                } else if (regOrd1 == null) {
                    regOrd1 = regRes;
                    ra.map(regOrd1, symbolOrd1);
                    pushBackOrLoadFromMem(regOrd1, symbolOrd1, 0, Instruction.LS.Op.lw);
                }
            } else if (!ord1.equals("0")) {
                 if (op != Code.Op.ADD) {
                    regOrd1 = "$a0";
                    mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, regOrd1, Integer.valueOf(ord1))));
                } else {
                    tag = 1;
                }
            } else {
                regOrd1 = "$zero";
            }
            // ord2
            /*冲突*/
            if (symbolOrd2 != null) {
                regOrd2 = ra.find(symbolOrd2);
                if (regOrd2 == null && !symbolOrd2.equals(symbolRes)) {
                    regOrd2 = ra.alloc(symbolOrd2, true, regOrd1, true);
                    pushBackOrLoadFromMem(regOrd2, symbolOrd2, 0, Instruction.LS.Op.lw);
                } else if (regOrd2 == null) {
                    regOrd2 = regRes;
                    ra.map(regOrd2, symbolOrd2);
                    pushBackOrLoadFromMem(regOrd2, symbolOrd2, 0, Instruction.LS.Op.lw);
                }
            } else if (op != Code.Op.NOT && !ord2.equals("0")) { // !!!
                if (op != Code.Op.SUB && op != Code.Op.ADD) {
                    regOrd2 = "$a0";
                    mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, regOrd2, Integer.valueOf(ord2))));
                } else {
                    tag = 2;
                }
            } else {
                regOrd2 = "$zero";
            }

            if (tag == 0) {
                switch (op) {
                    case ADD:
                        mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, regRes, regOrd1, regOrd2)));
                        break;
                    case SUB:
                        mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.subu, regRes, regOrd1, regOrd2)));
                        break;
                    case MUL:
                        mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.mul, regRes, regOrd1, regOrd2)));
                        break;
                    case DIV:
                        mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.div, regOrd1, regOrd2)));
                        mipsCodeList.add(String.valueOf(new Instruction.M(Instruction.M.Op.mflo, regRes)));
                        break;
                    case MOD:
                        mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.div, regOrd1, regOrd2)));
                        mipsCodeList.add(String.valueOf(new Instruction.M(Instruction.M.Op.mfhi, regRes)));
                        break;
                    case EQ:
                    case NOT:
                        mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.seq, regRes, regOrd1, regOrd2)));
                        break;
                    case NE:
                        mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.sne, regRes, regOrd1, regOrd2)));
                        break;
                    case GT:
                        mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.sgt, regRes, regOrd1, regOrd2)));
                        break;
                    case GE:
                        mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.sge, regRes, regOrd1, regOrd2)));
                        break;
                    case LT:
                        mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.slt, regRes, regOrd1, regOrd2)));
                        break;
                    case LE:
                        mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.sle, regRes, regOrd1, regOrd2)));
                        break;
                }
            } else {
                String reg;
                int num;
                if (tag == 1) {
                    reg = regOrd2;
                    num = Integer.parseInt(ord1);
                } else {
                    reg = regOrd1;
                    num = Integer.parseInt(ord2);
                }
                switch (op) {
                    case ADD:
                        mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, regRes, reg, num)));
                        break;
                    case SUB:
                        mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, regRes, reg, -num)));
                        break;
                }
            }
        }
        // 防止regOrd1 或者 regOrd2 覆盖了 regRes
        ra.map(regRes, symbolRes);
        ra.setDirty(regRes);
    }

    /** 传值不会是数组变量
     * 栈针还是当前函数
     * 注意如果是该函数形参压栈 ！！！
     * */
    public void pushIntoStack(Integer cnt, String param, Symbol paramSymbol, boolean isAdd, Integer funSize) {
        String reg = null;
        if (!isAdd) {
            if (paramSymbol != null) reg = ra.find(paramSymbol);
            if (reg == null && paramSymbol != null) {
                reg = ra.alloc(paramSymbol, true, null, true);
                pushBackOrLoadFromMem(reg, paramSymbol, 0, Instruction.LS.Op.lw);
            } else if (reg == null) {
                if (Integer.valueOf(param) != 0) {
                    reg = "$a0";
                    mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, reg, Integer.valueOf(param))));
                } else reg = "$zero";
            }
        } else {
            reg = "$a1";
            Matcher m = Code.indexPattern.matcher(param);
            String strOff = null;
            if (m.matches()) strOff = m.group(1);
            if (strOff != null && "-0123456789".indexOf(strOff.charAt(0)) == -1) {
                loadLVal("$a1", strOff);
                mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sll, "$a1", "$a1", 2)));
            } else {
                assert strOff != null;
                mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$a1", Integer.parseInt(strOff) * 4)));
            }
            int blockLevel = paramSymbol.getBlockLevel();
            /* 如果是该函数形参 ！！！*/
            if (paramSymbol instanceof FuncFormParam) {
                String addReg = ra.find(paramSymbol);
                if (addReg == null) {
                    addReg = ra.alloc(paramSymbol, true, null, true);
                    mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.lw, addReg, null, paramSymbol.getAddr(), "$sp")));
                }
                mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, "$a1", "$a1", addReg)));
            } else if (blockLevel == 0 || (paramSymbol instanceof Val && ((Val) paramSymbol).isArrayInMain)) {
                mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$a1", "$a1", globalArrAddr.get(paramSymbol) + 0x10010000)));
            } else {
                mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$a1", "$a1", paramSymbol.getAddr())));
                mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, "$a1", "$a1", "$sp")));
            }
        }
        mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.sw, reg, null, -32 * 4 - funSize + cnt * 4, "$sp")));
    }

    /**
     * 栈针还是当前函数
     * 如果是形参且为地址那么不用回写，因为一定为常数，否则会将地址写到地址处
     * 2
     * 1
     * 0
     */
    public void saveRegs(Integer codeId, String func, HashMap<String, Symbol> used) {
        HashSet<Symbol> activeOut;
        if (!optimize.get("CountVal")) activeOut = null;
        else activeOut = DataFlow.getAco(func, codeId);
        for (String reg : used.keySet()) {
            // 如果是形参且为地址那么不用回写，因为一定为常数，否则会将地址写到地址处
            Symbol sym = ra.regMap.get(reg);
            if (sym.getBlockLevel() != 0 && (activeOut == null || activeOut.contains(sym))) {
                if (!(sym instanceof FuncFormParam) || sym.getDim() == 0) {
                    if (!optimize.get("CountVal") || !(val2Used.containsKey(sym) && val2Used.get(sym) == 0 && !ra.inGraph(sym))) {
                        if (ra.isDirty(reg) || ra.inGraph(reg)) ra.saveToMem(reg);
                    }
                }
            }
        }
    }

    public void returnFromFunc(String val, int funSize, boolean lw, boolean recover) {
        if (val != null && !val.equals("(EMPTY)")) loadLVal("$v0", val);
        if (lw && recover)
            mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.lw, "$ra", null, funSize + 31 * 4, "$sp")));
        mipsCodeList.add(String.valueOf(new Instruction.M(Instruction.M.Op.jr, "$ra")));
    }

    // ---------------------------- TRANSLATE ---------------------------------------------------------------
    // gp:          sp:
    // -------------------
    // 1 <-- gp     3
    // 2            2
    // 3            1 <-- sp
    public void translate() {
        mipsCodeList.add(".data");
        // global arr
        int addr = 0;
        for (Symbol sym : Visitor.global.getOrderSymbols()) {
            if (sym instanceof Val && (sym.getDim() != 0 || optimize.get("GlobalPositionOptimize"))) {
                globalArrAddr.put(sym, addr);
                mipsCodeList.add(Instruction.wordOrSpace(getName(sym), ((Val) sym).getInitVal(), ((Val) sym).isHasInitVal()));
                addr += sym.getSize();
            }
        }
        for (Val val : Visitor.arrayInMain) {
            globalArrAddr.put(val, addr);
            mipsCodeList.add(Instruction.wordOrSpace(getName(val), val.getInitVal(), val.isHasInitVal()));
            addr += val.getSize();
        }
        // str
        for (int i = 0; i < MidCodeList.strings.size(); ++i) {
            mipsCodeList.add(Instruction.asciiz("STR" + i, MidCodeList.strings.get(i)));
        }
        // code
        mipsCodeList.add(".text");
        // global val
        if (!optimize.get("GlobalPositionOptimize")) {
            if (Visitor.global.totSize != 0)
                mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$gp", "$gp", Visitor.global.totSize)));
            for (Symbol sym : Visitor.global.getOrderSymbols()) {
                if (sym instanceof Val && sym.getDim() == 0 && ((Val) sym).isHasInitVal()) {
                    if (((Val) sym).getInitVal().get(0) != 0) {
                        mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$a0", ((Val) sym).getInitVal().get(0))));
                        mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.sw, "$a0", null, -sym.getAddr(), "$gp")));
                    } else {
                        mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.sw, "$zero", null, -sym.getAddr(), "$gp")));
                    }
                }
            }
        }
        Func mainFunc = ((Func) Visitor.str2Symbol.get("main(0,1)"));
        mainFunc.ca = new ColorAlloc("main(0,1)", DataFlow.func2blocks.get("main(0,1)"));
        mainFunc.ca.alloc();
        for (ArrayList<Code> fun : funcList) {
            Func func = (Func) fun.get(0).getSymbolOrd1();
            func.ca = new ColorAlloc(func.getNickname(), DataFlow.func2blocks.get(func.getNickname()));
            func.ca.alloc();
        }
        translateFunc(mainList);
        for (ArrayList<Code> fun : funcList) translateFunc(fun);
    }

    public void translateFunc(ArrayList<Code> funCodeList) {
        Func func = (Func) funCodeList.get(0).getSymbolOrd1();
        ra = new RegAlloc(func.getNickname());
        mipsCodeList.add(getName(func) + ":");

        if (func.getName().startsWith("main"))
            mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$sp", "$sp", -func.getFuncTable().totSize)));
        else if (func.callOtherFunc)
            mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.sw, "$ra", null, func.getFuncTable().totSize + 31 * 4, "$sp")));

        int paraNum = 0;
        //int toRegParaNum = 0;
        Stack<Func> callFunc = new Stack<>();
        //HashMap<String, String> toRegPara = new HashMap<>();

        HashMap<Integer, Integer> blockEnd = DataFlow.getBlockEnd(func.getNickname());
        HashMap<Integer, Integer> blockBegin = DataFlow.getBlockBegin(func.getNickname());


        ArrayList<Symbol> symbols = new ArrayList<Symbol>(func.getFuncTable().getOrderSymbols().subList(0, func.getNum())); // 形参
        ColorAlloc ca = func.ca;
        for (Symbol s : symbols) {
            if (ca.mp.containsKey(s)) {
                int rid = ca.regAlloc[ca.mp.get(s)];
                if (rid != -1) {
                    String reg = RegAlloc.Regs[rid];
                    pushBackOrLoadFromMem(reg, s, 0, Instruction.LS.Op.lw);
                    ra.find(s); // 手动建立映射
                }
            }
        }

        for (int i = 1; i < funCodeList.size(); ++i) {

            mipsCodeList.add("\n");
            mipsCodeList.add("#" + funCodeList.get(i));
            Code code = funCodeList.get(i);
            Code.Op op = code.getInstr();
            String ord1 = code.getOrd1(), ord2 = code.getOrd2(), res = code.getRes();
            if (blockBegin.containsKey(i)) ra.clkRefresh();
            if (blockEnd.containsKey(i) && op != Code.Op.RETURN && op != Code.Op.FUNC_END && Code.jump.contains(op) && op != Code.Op.LABEL)
                saveRegs(i, func.getNickname(), ra.clkGetAllUsed());

            // 标记临时变量的使用次数
            Symbol o1 = hasTmp(ord1), o2 = hasTmp(ord2), r = hasTmp(res);
            if (optimize.get("CountVal")) {
                if (o1 != null) val2Used.merge(o1, -1, Integer::sum);
                if (o2 != null) val2Used.merge(o2, -1, Integer::sum);
                if (r != null) val2Used.merge(r, -1, Integer::sum);
            }

            // def: arr_def 和 end_arr_def 直接跳过即可
            if (op == Code.Op.DEF_VAL) {
                ra.alloc(code.getSymbolRes(), false, null, true);
                if (!ord1.equals("(EMPTY)")) saveLVal(ord1, res);
            } else if (op == Code.Op.ASSIGN) {
                if (Objects.equals(ord1, "(RT)")) ord1 = "$v0";
                saveLVal(ord1, res);
                if (code.getSymbolRes() != null && code.getSymbolRes().getDim() == 0 && code.getSymbolRes().getBlockLevel() == 0) {
                    String reg = ra.find(code.getSymbolRes());
                    if (reg != null && ra.isDirty(reg)) ra.saveToMem(reg);
                }
            } else if (Code.io.contains(op)) {
                if (op == Code.Op.GET_INT) {
                    getInt(res);
                    if (code.getSymbolRes() != null && code.getSymbolRes().getDim() == 0 && code.getSymbolRes().getBlockLevel() == 0) {
                        String reg = ra.find(code.getSymbolRes());
                        if (reg != null && ra.isDirty(reg)) ra.saveToMem(reg);
                    }
                } else if (op == Code.Op.PRINT_INT) printInt(ord1);
                else printStr(ord1);
            } else if (Code.alu.contains(op)) { // not没有
                alu(op, res, ord1, ord2, code.getSymbolRes(), code.getSymbolOrd1(), code.getSymbolOrd2());
                if (code.getSymbolRes() != null && code.getSymbolRes().getDim() == 0 && code.getSymbolRes().getBlockLevel() == 0) {
                    String reg = ra.find(code.getSymbolRes());
                    if (reg != null && ra.isDirty(reg)) ra.saveToMem(reg);
                }
            } else if (Code.func.contains(op)) {
                if (op == Code.Op.PREPARE_CALL) {
                    callFunc.push((Func) code.getSymbolOrd1());
                } else if (op == Code.Op.PUSH_PAR_INT) {
                    //ColorAlloc oca = callFunc.lastElement().ca;
                    //ArrayList<Symbol> oSymbols = new ArrayList<Symbol>(callFunc.lastElement().getFuncTable().getOrderSymbols().subList(0, callFunc.lastElement().getNum())); // 形参
                    //int k = 0, tag = 0;
                    //for (int j = 0; j < oSymbols.size(); ++j) {
                    //   Symbol s = oSymbols.get(j);
                    //   if (oca.mp.containsKey(s) && oca.regAlloc[oca.mp.get(s)] != -1) {
                    //        ++k;
                    //        tag = j;
                    //    }
                    //}
                    //if (!k) pushIntoStack(paraNum++, ord1, code.getSymbolOrd1(), false, callFunc.lastElement().getFuncTable().totSize);
                    //else {
                    //    paraNum++;
                    //    toRegParaNum++;
                    //    ca.param.put(s, ca.regAlloc[ca.mp.get(s)]);
                    //    toRegPara.put(ord1, RegAlloc.Regs[ca.regAlloc[ca.mp.get(s)]]);
                    //}
                    pushIntoStack(paraNum++, ord1, code.getSymbolOrd1(), false, callFunc.lastElement().getFuncTable().totSize);
                } else if (op == Code.Op.PUSH_PAR_ADDR) {
                    pushIntoStack(paraNum++, ord1, code.getSymbolOrd1(), true, callFunc.lastElement().getFuncTable().totSize);
                } else if (op == Code.Op.RETURN) {
                    if (func.getName().startsWith("main")) {
                        mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$v0", 10)));
                        mipsCodeList.add(String.valueOf(new Instruction.NP(Instruction.NP.Op.syscall)));
                    } else
                        returnFromFunc(ord1, func.getFuncTable().totSize, func.callOtherFunc, DataFlow.mayThroughCall(func.getNickname(), i));
                } else if (op == Code.Op.CALL) {
                    paraNum = 0;
                    HashSet<Symbol> aco = DataFlow.getAco(func.getNickname(), i);

                    // 恢复参数
                    HashMap<String, Symbol> used = ra.getAllUsed();
                    for (Symbol s : symbols) {
                        if (ca.mp.containsKey(s)) {
                            int rid = ca.regAlloc[ca.mp.get(s)];
                            if (rid != -1) {
                                String reg = RegAlloc.Regs[rid];
                                if (used.containsKey(reg) && aco.contains(s)) ra.find(s);
                            }
                        }
                    }

                    // 获取要恢复的寄存器
                    HashMap<Symbol, Boolean> t = ra.getGraphVar();
                    HashMap<Symbol, Boolean> recover = new HashMap<>();
                    for (Symbol s : aco) {
                        if (t.containsKey(s)) recover.put(s, t.get(s));
                    }

                    // 保存到内存
                    saveRegs(i, func.getNickname(), ra.getAllUsed());
                    ra.clkRefresh();

                    mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$sp", "$sp", -callFunc.lastElement().getFuncTable().totSize - 32 * 4)));
                    mipsCodeList.add("jal " + getName(callFunc.lastElement()));
                    mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$sp", "$sp", 32 * 4 + callFunc.lastElement().getFuncTable().totSize)));

                    // 恢复寄存器
                    for (Symbol s : recover.keySet()) {
                        String reg = ra.find(s);
                        mipsCodeList.add("#" + reg + " <--- " + s.getName());
                        pushBackOrLoadFromMem(reg, s, 0, Instruction.LS.Op.lw);
                    }

                    callFunc.pop();
                    //toRegParaNum = 0;
                    //toRegPara.clear();
                }
            } else if (Code.jump.contains(op)) {
                if (op == Code.Op.JUMP) {
                    mipsCodeList.add(String.valueOf(new Instruction.L(Instruction.L.Op.j, ord1.substring(1, ord1.length() - 1))));
                } else if (op == Code.Op.EQZ_JUMP || op == Code.Op.NEZ_JUMP) {
                    String reg = ra.find(code.getSymbolOrd1());
                    if (reg == null) {
                        loadLVal("$a1", ord1);
                        reg = "$a1";
                    }
                    mipsCodeList.add(String.valueOf(new Instruction.ML(op == Code.Op.EQZ_JUMP ? Instruction.ML.Op.beqz : Instruction.ML.Op.bnez, reg, res.substring(1, res.length() - 1))));
                } else if (op == Code.Op.LABEL) mipsCodeList.add(ord1.substring(1, ord1.length() - 1) + ":");
            }
            if (optimize.get("CountVal")) {
                if (o1 != null && val2Used.get(o1) == 0 && !ra.inGraph(code.getSymbolOrd1())) {
                    String reg = ra.find(code.getSymbolOrd1());
                    if (reg != null) ra.refreshOne(reg);
                }
                if (o2 != null && val2Used.get(o2) == 0 && !ra.inGraph(code.getSymbolOrd2())) {
                    String reg = ra.find(code.getSymbolOrd2());
                    if (reg != null) ra.refreshOne(reg);
                }
                if (r != null && val2Used.get(r) == 0 && !ra.inGraph(code.getSymbolRes())) {
                    String reg = ra.find(code.getSymbolRes());
                    if (reg != null) ra.refreshOne(reg);
                }
            }
            if (blockEnd.containsKey(i) && op != Code.Op.RETURN && op != Code.Op.FUNC_END && (!Code.jump.contains(op) || op == Code.Op.LABEL))
                saveRegs(i, func.getNickname(), ra.clkGetAllUsed());
        }
    }

    public String printMipsCode(boolean notation) {
        StringBuilder sb = new StringBuilder();
        for (String s : mipsCodeList) {
            if (!notation && (s.startsWith("#") || s.startsWith("\n") || s.length() == 0)) continue;
            sb.append(s).append("\n");
        }
        return sb.toString();
    }
}
