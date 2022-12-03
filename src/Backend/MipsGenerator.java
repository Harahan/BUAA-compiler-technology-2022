package Backend;

import Backend.Util.Instruction;
import Backend.Optimization.PeepHole;
import Backend.Util.RegAlloc;
import Backend.Optimization.*;
import Middle.MidCodeList;
import Middle.Optimization.DataFlow;
import Middle.Util.Code;
import Middle.Visitor;
import Symbol.Symbol;
import Symbol.SymbolTable;
import Symbol.FuncFormParam;
import Symbol.Func;
import Symbol.Tmp;
import Symbol.Val;
import Backend.Util.RegAlloc.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MipsGenerator {
    public static HashMap<String, Boolean> optimize = new HashMap<String, Boolean>() {{
        put("MulDiv", true);
        put("DeleteDeadCode", false);
        put("PeepHole", true);
        put("BroadcastCode", true); // const and val both
        put("TmpRegisterAlloc", true);
        put("MidCodeOptimize", true);
        put("JumpOptimize", true);
        put("RemoveRedundantCall", true);
        put("ExtractLoopConstExp", true);
    }};

    public static ArrayList<String> mipsCodeList = new ArrayList<>();
    private final ArrayList<ArrayList<Code>> funcList = new ArrayList<>();
    private ArrayList<Code> mainList = new ArrayList<>();
    private final HashMap<Symbol, Integer> globalArrAddr = new HashMap<>();
    public static HashMap<String, Integer> tmpVal2Used = new HashMap<>();

    public static final Pattern tempPattern = Pattern.compile(".*\\(T(\\d+)\\).*");

    public MipsGenerator(ArrayList<Code> codes) {
        // System.out.println(codes);

        if (optimize.get("TmpRegisterAlloc")) {
            for (Code code : codes) {
                String ord1 = hasTmp(code.getOrd1());
                String ord2 = hasTmp(code.getOrd2());
                String res = hasTmp(code.getRes());
                if (ord1 != null) tmpVal2Used.merge(ord1, 1, Integer::sum);
                if (ord2 != null) tmpVal2Used.merge(ord2, 1, Integer::sum);
                if (res != null) tmpVal2Used.merge(res, 1, Integer::sum);
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (codes.get(i).getInstr() == Code.Op.FUNC) {
                String name = codes.get(i).getOrd1();
                ArrayList<Code> func = new ArrayList<>();
                while (codes.get(i).getInstr() != Code.Op.FUNC_END) func.add(codes.get(i++));
                func.add(codes.get(i));
                if (name.startsWith("main")) mainList = func;
                else  funcList.add(func);
            }
        }
        setOff(Visitor.global, 0);
        translate();
        if (optimize.get("PeepHole")) PeepHole.peepHole(mipsCodeList);
    }

    // ------------------------------------- PREPARE ------------------------------------------------------------

    public static String getName(Symbol sym) {
        return sym.getName() + "_" + sym.getBlockLevel() + "_" + sym.getBlockNum();
    }

    public static String hasTmp(String var) {
        Matcher matcher = tempPattern.matcher(var);
        if (matcher.matches()) {
            return "(T" + matcher.group(1) + ")";
        }
        return null;
    }

    public int setOff(SymbolTable nd, int off) {
        // System.out.println(nd);
        int init = off;
        for (int i = 0; i < nd.getOrderSymbols().size(); ++i) {
            Symbol sym = nd.getOrderSymbols().get(i);
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

    // -----------------------------------------------
    // reg <---> mem(sym + off(T))
    // reg <---> mem(sym + off(int))
    // int ---> mem(sym + off(T))
    // int ---> mem(sym + off(int))

    // off --> val: 0
    // off --> arr: 数组内偏移量 * 4
    // off为数字
    // gp: - sp: +
    public static void pushBackOrLoadFromMem(String reg, Symbol sym, Integer off, Instruction.LS.Op op) {
        // 处理临时变量回写，不会有问题
        // T0 --> a[T1]
        // xxx
        // saveRegs 单独处理
        if (optimize.get("TmpRegisterAlloc")) {
            if (sym instanceof Tmp && tmpVal2Used.get(sym.getName()) == 0 && op == Instruction.LS.Op.sw) {
                RegAlloc.refreshOne(reg);
                return;
            }
        }

        //if (reg == "$v0") System.out.println(sym + " " + sym.getBlockLevel());
        if (sym instanceof Tmp) {
            mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, off + sym.getAddr(), "$sp")));
        }
        if (sym instanceof Val) {
            Val val = (Val) sym;
            if (val.getBlockLevel() != 0) {
                // 局部变量
                // if (reg == "$v0") System.out.println(reg);
                mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, off + sym.getAddr(), "$sp")));
            } else {
                // 全局变量
                if (val.getDim() == 0) {
                    mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, -sym.getAddr(), "$gp")));
                } else {
                    mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, getName(sym), off, "$zero")));
                }
            }
        }
        if (sym instanceof FuncFormParam && sym.getDim() > 0) {
            // System.out.println(sym);
            String addReg = RegAlloc.find(sym, 0);
            if (addReg == null) {
                // 不在寄存器中
                addReg = RegAlloc.mandatoryAllocOne(reg, sym, 0, true);
                mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.lw, addReg, null, sym.getAddr(), "$sp")));
            }
            // System.out.println(reg + " " + sym);
            mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, off, addReg)));
        } else if (sym instanceof FuncFormParam) {
            // off == 0
            mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, off + sym.getAddr(), "$sp")));
        }
    }

    // off --> val: 0
    // off --> arr: 数组内偏移量 * 4
    // off为临时变量
    // gp: - sp: +
    public static void pushBackOrLoadFromMem(String reg, Symbol sym, Symbol off, Instruction.LS.Op op) {
        String tmpReg = RegAlloc.find(off, 0);
        if (tmpReg == null) {
            tmpReg = RegAlloc.mandatoryAllocOne(reg, off, 0, true);
            pushBackOrLoadFromMem(tmpReg, off, 0, Instruction.LS.Op.lw);
        }
        mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sll, "$a1", tmpReg, 2)));
        tmpReg = "$a1";

        if (sym instanceof Val) {
            Val val = (Val) sym;
            if (val.getBlockLevel() != 0) {
                // 局部变量
                mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, tmpReg, tmpReg, sym.getAddr())));
                mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, tmpReg, tmpReg, "$sp")));
                mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, 0, tmpReg)));
            } else {
                // 全局变量
                mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, getName(sym), 0, tmpReg)));
            }
        }
        if (sym instanceof FuncFormParam) {
            String addReg = RegAlloc.find(sym, 0);
            if (addReg == null) {
                // 不在寄存器中
                addReg = RegAlloc.mandatoryAllocOne(reg, sym, 0, true);
                mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.lw, addReg, null, sym.getAddr(), "$sp")));
            }
            mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, tmpReg, tmpReg, addReg)));
            mipsCodeList.add(String.valueOf(new Instruction.LS(op, reg, null, 0, tmpReg)));
        }
    }

    public static void pushBackMem(Integer imm, Symbol sym, Integer off) {
        mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$a0", imm)));
        pushBackOrLoadFromMem("$a0", sym, off, Instruction.LS.Op.sw);
    }

    public static void pushBackMem(Integer imm, Symbol sym, Symbol off) {
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
                // System.out.println(m.group(1));
                symOffLVal = Visitor.str2Symbol.get(m.group(1));
            }
        }

        //  reg --> lVal
        assert symbolLVal != null;
        if (rVal.startsWith("$")) {
            if (symbolLVal.getDim() == 0) {
                String resReg = RegAlloc.find(symbolLVal, 0);
                // if (rVal == "$v0") System.out.println(resReg);
                if (resReg != null) {
                    mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, resReg, rVal)));
                    RegAlloc.dirtyRegs.put(resReg, true);
                }
                else pushBackOrLoadFromMem(rVal, symbolLVal, 0, Instruction.LS.Op.sw);
            } else if (immOffLVal != null) {
                pushBackOrLoadFromMem(rVal, symbolLVal, immOffLVal * 4, Instruction.LS.Op.sw);
            } else if (symOffLVal != null) {
                pushBackOrLoadFromMem(rVal, symbolLVal, symOffLVal, Instruction.LS.Op.sw);
            }
            return;
        }

        // number --> lVal
        if ("-0123456789".indexOf(rVal.charAt(0)) != -1) {
            if (immOffLVal != null) pushBackMem(Integer.valueOf(rVal), symbolLVal, immOffLVal * 4);
            else if (symOffLVal != null) pushBackMem(Integer.valueOf(rVal), symbolLVal, symOffLVal);
            else {
                String reg = RegAlloc.find(symbolLVal, 0);
                if (reg == null) pushBackMem(Integer.valueOf(rVal), symbolLVal, 0);
                else {
                    mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, reg, Integer.valueOf(rVal))));
                    RegAlloc.dirtyRegs.put(reg, true);
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
            String src = RegAlloc.find(symbolRVal, 0);
            if (src == null) {
                src = RegAlloc.mandatoryAllocOne(symbolRVal, 0, true);
                pushBackOrLoadFromMem(src, symbolRVal, 0, Instruction.LS.Op.lw);
            }

            if (symOffLVal == null && immOffLVal == null) {
                // lVal: 0 rVal: 0
                String reg = RegAlloc.find(symbolLVal, 0);
                if (reg != null) {
                    mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, reg, src)));
                    RegAlloc.dirtyRegs.put(reg, true);
                }
                else pushBackOrLoadFromMem(src, symbolLVal, 0, Instruction.LS.Op.sw);

            } else if (symOffLVal != null) {
                pushBackOrLoadFromMem(src, symbolLVal, symOffLVal, Instruction.LS.Op.sw);
            } else {
                pushBackOrLoadFromMem(src, symbolLVal, immOffLVal * 4, Instruction.LS.Op.sw);
            }

        } else {
            String src = RegAlloc.find(symbolLVal, 0);
            if (src == null) {
                src = RegAlloc.mandatoryAllocOne(symbolLVal, 0, true);
            }
            RegAlloc.dirtyRegs.put(src, true);
            if (symOffRVal != null) pushBackOrLoadFromMem(src, symbolRVal, symOffRVal, Instruction.LS.Op.lw);
            else if (immOffRVal != null) pushBackOrLoadFromMem(src, symbolRVal, immOffRVal * 4, Instruction.LS.Op.lw);
        }
    }

    /**
     * @param reg: reg
     * @param lVal: number, val, funcFormParam, reg
     * tips: 如果lVal在寄存器中或者为寄存器，不会对reg建立和lVal(或者lVal中存储变量)的映射, 对于funFormParam的数组直接从内存具体索引加载值
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

        String regLVal = RegAlloc.find(symbolLVal, 0);
        if (regLVal != null) mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, reg, regLVal)));
        else if (symOffLVal == null && immOffLVal == null) pushBackOrLoadFromMem(reg, symbolLVal, 0, Instruction.LS.Op.lw);
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
        if (optimize.get("MulDiv") && (((op == Code.Op.MOD || op == Code.Op.DIV) && ord1.equals(res) && symbolOrd2 == null)
                || (op == Code.Op.MUL && ((ord2.equals(res) && symbolOrd1 == null) || (ord1.equals(res) && symbolOrd2 == null))))) {
            regRes = RegAlloc.mandatoryAllocOne(symbolRes, 0, false);
        } else {
            regRes = RegAlloc.find(symbolRes, 0);
            if (regRes == null) {
                regRes = RegAlloc.mandatoryAllocOne(symbolRes, 0, false);
            }
        }

        if (symbolOrd1 == null && symbolOrd2 == null && op != Code.Op.NOT) {
            // 数字+数字
            Integer intOrd1 = Integer.valueOf(ord1), intOrd2 = Integer.valueOf(ord2);
            Integer ans = 0;
            switch (op) {
                case ADD: ans = intOrd1 + intOrd2; break;
                case SUB: ans = intOrd1 - intOrd2; break;
                case MUL: ans = intOrd1 * intOrd2; break;
                case DIV: ans = intOrd1 / intOrd2; break;
                case MOD: ans = intOrd1 % intOrd2; break;
                case EQ: ans = (intOrd1.equals(intOrd2) ? 1 : 0); break;
                case NE: ans = (!intOrd1.equals(intOrd2) ? 1 : 0); break;
                case GT: ans = (intOrd1 > intOrd2 ? 1 : 0); break;
                case GE: ans = (intOrd1 >= intOrd2 ? 1 : 0); break;
                case LT: ans = (intOrd1 < intOrd2 ? 1 : 0); break;
                case LE: ans = (intOrd1 <= intOrd2 ? 1 : 0); break;
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
                        String x = RegAlloc.find(symbolVal, 0);
                        if (x == null) {
                            x = RegAlloc.mandatoryAllocOne(regRes, symbolVal, 0, false);
                            loadLVal(x, val);
                            RegAlloc.mandatorySet(x, symbolVal, 0);
                        }
                        if (num == -1) {
                            mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.neg, regRes, x)));
                            RegAlloc.mandatorySet(regRes, symbolRes, 0);
                            RegAlloc.dirtyRegs.put(regRes, true);
                            return;
                        }
                        mipsCodeList.addAll(MulDiv.mul(regRes, x, num, 4));
                    }
                    RegAlloc.mandatorySet(regRes, symbolRes, 0);
                    RegAlloc.dirtyRegs.put(regRes, true);
                    return;
                }
            }
            // div optimize
            if ((op == Code.Op.DIV || op == Code.Op.MOD) && symbolOrd2 == null && optimize.get("MulDiv")) {
                int d = Integer.parseInt(ord2);
                String x = RegAlloc.find(symbolOrd1, 0);
                if (x == null) {
                    x = RegAlloc.mandatoryAllocOne(regRes, symbolOrd1, 0, false);
                    loadLVal(x, ord1);
                    RegAlloc.mandatorySet(x, symbolOrd1, 0);
                }
                mipsCodeList.addAll(MulDiv.div(regRes, x, d, 50, op == Code.Op.DIV, symbolOrd1));
                RegAlloc.mandatorySet(regRes, symbolRes, 0);
                RegAlloc.dirtyRegs.put(regRes, true);
                return;
            }
            // 零维变量 @ 零维变量
            String regOrd1 = null, regOrd2 = null;
            int tag = 0;
            // ord1
            if (symbolOrd1 != null) {
                regOrd1 = RegAlloc.find(symbolOrd1, 0);
                if (regOrd1 == null) {
                    regOrd1 = RegAlloc.mandatoryAllocOne(symbolOrd1, 0, false);
                    loadLVal(regOrd1, ord1);
                    RegAlloc.mandatorySet(regOrd1, symbolOrd1, 0);
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
                regOrd2 = RegAlloc.find(symbolOrd2, 0);
                if (regOrd2 == null) {
                    regOrd2 = RegAlloc.mandatoryAllocOne(regOrd1, symbolOrd2, 0, false);
                    // System.out.println(ord2);
                    loadLVal(regOrd2, ord2);
                    RegAlloc.mandatorySet(regOrd2, symbolOrd2, 0);
                }
            } else if (op != Code.Op.NOT && !ord1.equals("0")) { // !!!
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
                        mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.subiu, regRes, reg, num)));
                        break;
                    }
            }
        }
        // 防止regOrd1 或者 regOrd2 覆盖了 regRes
        RegAlloc.mandatorySet(regRes, symbolRes, 0);
        RegAlloc.dirtyRegs.put(regRes, true);
    }

    /** 传值不会是数组变量
     * 栈针还是当前函数
     * 注意如果是该函数形参压栈 ！！！
     * */
    public void pushIntoStack(Integer cnt, String param, Symbol paramSymbol, boolean isAdd, Integer funSize) {
        String reg = null;
        if (!isAdd) {
            if (paramSymbol != null) reg = RegAlloc.find(paramSymbol, 0);
            if (reg == null) {
                reg = RegAlloc.mandatoryAllocOne(paramSymbol, 0, false);
                loadLVal(reg, param);
            }
        } else {
            reg = "$a1";
            Matcher m = Code.indexPattern.matcher(param);
            String strOff = null;
            if (m.matches()) strOff = m.group(1);
            if (strOff != null) {
                loadLVal("$a1", strOff);
                mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sll, "$a1", "$a1", 2)));
            } else {
                mipsCodeList.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, "$a1", "$zero")));
            }
            int blockLevel = paramSymbol.getBlockLevel();
            /* 如果是该函数形参 ！！！*/
            if (paramSymbol instanceof FuncFormParam) {
                String addReg = RegAlloc.find(paramSymbol, 0);
                if (addReg == null) {
                    // 不在寄存器中
                    addReg = RegAlloc.mandatoryAllocOne(paramSymbol, 0, true);
                    mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.lw, addReg, null, paramSymbol.getAddr(), "$sp")));
                }
                mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, "$a1", "$a1", addReg)));
            } else if  (blockLevel == 0) {
                mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$a1", "$a1", globalArrAddr.get(paramSymbol) + 0x10010000)));
                // ...
            } else {
                mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$a1", "$a1", paramSymbol.getAddr())));
                mipsCodeList.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, "$a1", "$a1", "$sp")));
            }
        }
        mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.sw, reg, null, -32 * 4 - funSize + cnt * 4, "$sp")));
    }

    /**
     *  栈针还是当前函数
     *   如果是形参且为地址那么不用回写，因为一定为常数，否则会将地址写到地址处
     *  2
     *  1
     *  0
     */
    public void saveRegs(Integer codeId, String func) {
        HashMap<String, Pair<Symbol, Integer>> used = RegAlloc.getAllUsed();
        HashSet<Symbol> activeOut;
        if (!optimize.get("TmpRegisterAlloc")) activeOut = null;
        else activeOut = DataFlow.getActiveOut(func, codeId);
        for (String reg : used.keySet()) {
            // 如果是形参且为地址那么不用回写，因为一定为常数，否则会将地址写到地址处
            Symbol sym = RegAlloc.regMap.get(reg).getKey();
            if (sym.getBlockLevel() != 0 && (activeOut == null || activeOut.contains(sym))) {
                if (!(sym instanceof FuncFormParam) || sym.getDim() == 0) {
                    // 处理临时变量，且该变量使用超过一次
                    if (!optimize.get("TmpRegisterAlloc") || !(sym instanceof Tmp && tmpVal2Used.get(sym.getName()) == 0)) {
                        mipsCodeList.add("# " + reg + ": " + sym.getName() + " ---> MEM");
                        if (RegAlloc.dirtyRegs.get(reg)) {
                            RegAlloc.dirtyRegs.put(reg, false);
                            pushBackOrLoadFromMem(reg, sym, 0, Instruction.LS.Op.sw);
                        }
                    }
                }
            }
        }
    }

    public void returnFromFunc(String val, int funSize, boolean lw) {
        if (val != null) loadLVal("$v0", val);
        if (lw) mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.lw, "$ra", null, funSize + 31 * 4, "$sp")));
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
            if (sym instanceof Val && sym.getDim() != 0) {
                globalArrAddr.put(sym, addr);
                mipsCodeList.add(Instruction.wordOrSpace(getName(sym), ((Val) sym).getInitVal(), ((Val) sym).isHasInitVal()));
                addr += sym.getSize();
            }
        }
        // str
        for (int i = 0; i < MidCodeList.strings.size(); ++i) {
            mipsCodeList.add(Instruction.asciiz("STR" + i, MidCodeList.strings.get(i)));
        }
        // code
        mipsCodeList.add(".text");
        // global val
        if (Visitor.global.totSize != 0) mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$gp", "$gp", Visitor.global.totSize)));
        for (Symbol sym : Visitor.global.getOrderSymbols()) {
            if (sym instanceof Val && sym.getDim() == 0 && ((Val) sym).isHasInitVal()) {
                mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$a0", ((Val)sym).getInitVal().get(0))));
                mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.sw, "$a0", null, -sym.getAddr(), "$gp")));
            }
        }
        translateFunc(mainList);
        for (ArrayList<Code> fun : funcList) translateFunc(fun);
    }

    public void translateFunc(ArrayList<Code> funCodeList) {
        Func func = (Func) funCodeList.get(0).getSymbolOrd1();
        mipsCodeList.add("\n");
        mipsCodeList.add(getName(func) + ":");
        if (func.getName().startsWith("main")) {
            mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$sp", "$sp", -func.getFuncTable().totSize)));
        } else if (func.callOtherFunc) {
            mipsCodeList.add(String.valueOf(new Instruction.LS(Instruction.LS.Op.sw, "$ra", null, func.getFuncTable().totSize + 31 * 4, "$sp")));
        }
        int paraNum = 0;
        /*函数调用嵌套*/
        Stack<Func> callFunc = new Stack<>();
        HashSet<Integer> blockEnd = DataFlow.getBlockEnd(func.getNickname());
        HashSet<Integer> blockBegin = DataFlow.getBlockBegin(func.getNickname());
        for (int i = 1; i < funCodeList.size(); ++i) {
            mipsCodeList.add("\n#" + funCodeList.get(i));
            Code code = funCodeList.get(i);
            Code.Op op = code.getInstr();
            String ord1 = code.getOrd1(), ord2 = code.getOrd2(), res = code.getRes();
            //if (code.toString().equals("EQZ_JUMP (T16) (LABEL19)")) System.out.println(RegAlloc.find(code.getSymbolOrd1(), 0));
            if (blockBegin.contains(i)) {
                for (String reg : RegAlloc.regMap.keySet()) {
                    RegAlloc.refreshOne(reg);
                }
            }
            //if (code.toString().equals("EQZ_JUMP (T16) (LABEL19)")) System.out.println(RegAlloc.find(code.getSymbolOrd1(), 0));
            if (blockEnd.contains(i) && op != Code.Op.RETURN && op != Code.Op.FUNC_END && Code.jump.contains(op) && op != Code.Op.LABEL) {
                saveRegs(i, func.getNickname());
            }
            //if (code.toString().equals("EQZ_JUMP (T16) (LABEL19)")) System.out.println(RegAlloc.find(code.getSymbolOrd1(), 0));

            // 标记临时变量的使用次数
            String o1 = hasTmp(ord1);
            String o2 = hasTmp(ord2);
            String r = hasTmp(res);
            if (optimize.get("TmpRegisterAlloc")) {
                if (o1 != null) tmpVal2Used.merge(o1, -1, Integer::sum);
                if (o2 != null) tmpVal2Used.merge(o2, -1, Integer::sum);
                if (r != null) tmpVal2Used.merge(r, -1, Integer::sum);
            }
            // def: arr_def 和 end_arr_def 直接跳过即可
            if (op == Code.Op.DEF_VAL) {
                RegAlloc.allocFreeOne(code.getSymbolRes(), 0);
                if (!ord1.equals("(EMPTY)")) saveLVal(ord1, res);
            } else if (op == Code.Op.ASSIGN) {
                if (Objects.equals(ord1, "(RT)")) ord1 = "$v0";
                saveLVal(ord1, res);
                // !!! 写回内存
                if (code.getSymbolRes() != null && code.getSymbolRes().getDim() == 0 && code.getSymbolRes().getBlockLevel() == 0) {
                    String reg = RegAlloc.find(code.getSymbolRes(), 0);
                    if (reg != null && RegAlloc.dirtyRegs.get(reg)) {
                        RegAlloc.dirtyRegs.put(reg, false);
                        pushBackOrLoadFromMem(reg, code.getSymbolRes(), 0, Instruction.LS.Op.sw);
                    }
                }
            } else if (Code.io.contains(op)) {
                if (op == Code.Op.GET_INT) getInt(res);
                else if (op == Code.Op.PRINT_INT) printInt(ord1);
                else printStr(ord1);
            } else if (Code.alu.contains(op)) { // not没有
                alu(op, res, ord1, ord2, code.getSymbolRes(), code.getSymbolOrd1(), code.getSymbolOrd2());
                // !!! 写回内存
                if (code.getSymbolRes() != null && code.getSymbolRes().getDim() == 0 && code.getSymbolRes().getBlockLevel() == 0) {
                    String reg = RegAlloc.find(code.getSymbolRes(), 0);
                    if (reg != null && RegAlloc.dirtyRegs.get(reg)) {
                        RegAlloc.dirtyRegs.put(reg, false);
                        pushBackOrLoadFromMem(reg, code.getSymbolRes(), 0, Instruction.LS.Op.sw);
                    }
                }
            } else if (Code.func.contains(op)) {
                if (op == Code.Op.PREPARE_CALL) {
                     mipsCodeList.add("\n");
                    callFunc.push((Func) code.getSymbolOrd1());
                } else if (op == Code.Op.PUSH_PAR_INT) {
                    pushIntoStack(paraNum++, ord1, code.getSymbolOrd1(), false, callFunc.lastElement().getFuncTable().totSize);
                } else if (op == Code.Op.PUSH_PAR_ADDR) {
                    pushIntoStack(paraNum++, ord1, code.getSymbolOrd1(), true, callFunc.lastElement().getFuncTable().totSize);
                } else if (op == Code.Op.RETURN) {
                    if (func.getName().startsWith("main")) {
                        mipsCodeList.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$v0", 10)));
                        mipsCodeList.add(String.valueOf(new Instruction.NP(Instruction.NP.Op.syscall)));
                    } else {
                        returnFromFunc(ord1, func.getFuncTable().totSize, func.callOtherFunc);
                    }
                } else if (op == Code.Op.CALL) {
                    paraNum = 0;
                    saveRegs(null, null);
                    for (String reg : RegAlloc.regMap.keySet()) {
                        RegAlloc.refreshOne(reg);
                    }
                    mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$sp", "$sp", -callFunc.lastElement().getFuncTable().totSize - 32 * 4)));
                    mipsCodeList.add("jal " +  getName(callFunc.lastElement()));
                    mipsCodeList.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$sp", "$sp", 32 * 4 + callFunc.lastElement().getFuncTable().totSize)));
                    mipsCodeList.add("\n");
                    callFunc.pop();
                }
            } else if (Code.jump.contains(op)) {
                // 跳转前压栈（部分执行？）
                if (op == Code.Op.JUMP) {
                    // saveRegs(i, func.getNickname());
                    mipsCodeList.add(String.valueOf(new Instruction.L(Instruction.L.Op.j, ord1.substring(1, ord1.length() - 1))));
                } else if (op == Code.Op.EQZ_JUMP || op == Code.Op.NEZ_JUMP) {
                    // saveRegs(i, func.getNickname());
                    String reg = RegAlloc.find(code.getSymbolOrd1(), 0);
                    if (reg == null) {
                        loadLVal("$a1", ord1);
                        reg = "$a1";
                    }
                    mipsCodeList.add(String.valueOf(new Instruction.ML(op == Code.Op.EQZ_JUMP ? Instruction.ML.Op.beqz : Instruction.ML.Op.bnez, reg, res.substring(1, res.length() - 1))));
                }  else if (op == Code.Op.LABEL) {
                    mipsCodeList.add(ord1.substring(1, ord1.length() - 1) + ":");
                }
            }
            // 释放临时变量所占据的寄存器
            if (optimize.get("TmpRegisterAlloc")) {
                if (o1 != null && tmpVal2Used.get(o1) == 0) {
                    String reg = RegAlloc.find(code.getSymbolOrd1(), 0);
                    if (reg != null) RegAlloc.refreshOne(reg);
                }
                if (o2 != null && tmpVal2Used.get(o2) == 0) {
                    String reg = RegAlloc.find(code.getSymbolOrd2(), 0);
                    if (reg != null) RegAlloc.refreshOne(reg);
                }
                if (r != null && tmpVal2Used.get(r) == 0) {
                    String reg = RegAlloc.find(code.getSymbolRes(), 0);
                    if (reg != null) RegAlloc.refreshOne(reg);
                }
            }

            if (blockEnd.contains(i) && op != Code.Op.RETURN && op != Code.Op.FUNC_END && (!Code.jump.contains(op) || op == Code.Op.LABEL)) {
                saveRegs(i, func.getNickname());
            }
            //if (code.toString().equals("GT vr5(1,6) 0 (T16)")) System.out.println(RegAlloc.find(code.getSymbolRes(), 0));
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
