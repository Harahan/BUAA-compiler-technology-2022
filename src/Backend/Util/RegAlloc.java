package Backend.Util;

import Backend.MipsGenerator;
import Middle.Visitor;
import Symbol.Func;
import Symbol.FuncFormParam;
import Symbol.Symbol;

import java.util.HashMap;

public class RegAlloc {
    public static final String[] Regs = new String[]{
            /*"$zero", "$at", "$v0",*/ "$v1",
            /*"$a0", $a1*/"$a2", "$a3",
            "$t0", "$t1", "$t2", "$t3",
            "$t4", "$t5", "$t6", "$t7",
            "$s0", "$s1", "$s2", "$s3",
            "$s4", "$s5", "$s6", "$s7",
            "$t8", "$t9", "$k0", "$k1",
            /*"$gp", "$sp",*/ "$fp" /*,"$ra"*/
    };
    public final ColorAlloc ca;
    public final HashMap<String, Boolean> dirtyRegs = new HashMap<>();
    public final HashMap<String, Symbol> regMap = new HashMap<>();
    public final HashMap<Symbol, String> graphMap = new HashMap<>();
    public int ptr = 0;
    public String[] clockRegs;

    public RegAlloc(String name) {
        this.ca = ((Func) Visitor.str2Symbol.get(name)).ca;
        clockRegs = new String[Regs.length - ca.tot];
        System.arraycopy(Regs, ca.tot, clockRegs, 0, Regs.length - ca.tot);
        for (Symbol sym : ca.mp.keySet()) {
            if (ca.regAlloc[ca.mp.get(sym)] != -1) graphMap.put(sym, Regs[ca.regAlloc[ca.mp.get(sym)]]);
        }
        for (String reg : Regs) {
            dirtyRegs.put(reg, false);
            regMap.put(reg, null);
        }
        //for (Symbol s : ca.param.keySet()) {
        //    regMap.put(Regs[ca.regAlloc[ca.mp.get(s)]], s);
        //    dirtyRegs.put(Regs[ca.regAlloc[ca.mp.get(s)]], true);
        //}
    }

    // ------- clock regs -------

    public void clkRefresh() {
        for (String reg : clockRegs) {
            dirtyRegs.put(reg, false);
            regMap.put(reg, null);
        }
    }

    public HashMap<String, Symbol> clkGetAllUsed() {
        HashMap<String, Symbol> used = new HashMap<>();
        for (String reg : clockRegs) {
            if (regMap.get(reg) != null) used.put(reg, regMap.get(reg));
        }
        return used;
    }

    private int nxt() {
        return (ptr + 1) % clockRegs.length;
    }

    public String clkAllocFreeOne(Symbol sym, String conflict, boolean mp) {
        int x = ptr;
        ptr = nxt();
        while (ptr != x && (regMap.get(clockRegs[ptr]) != null || clockRegs[ptr].equals(conflict))) ptr = nxt();
        if (regMap.get(clockRegs[ptr]) == null && !clockRegs[ptr].equals(conflict)) {
            if (mp) regMap.put(clockRegs[ptr], sym);
            MipsGenerator.mipsCodeList.add("#" + clockRegs[ptr] + " <--- " + sym.getName());
            return clockRegs[ptr];
        }
        ptr = nxt();
        while (ptr != x && (dirtyRegs.get(clockRegs[ptr]) || clockRegs[ptr].equals(conflict))) ptr = nxt();
        if (!dirtyRegs.get(clockRegs[ptr]) && !clockRegs[ptr].equals(conflict)) {
            if (mp) regMap.put(clockRegs[ptr], sym);
            MipsGenerator.mipsCodeList.add("#" + clockRegs[ptr] + " <--- " + sym.getName());
            return clockRegs[ptr];
        }
        return null;
    }

    public String clkMandatoryAlloc(Symbol sym, String conflict, boolean mp) {
        String reg = clkAllocFreeOne(sym, conflict, mp);
        if (reg != null) return reg;
        ptr = nxt();
        if (clockRegs[ptr].equals(conflict)) ptr = nxt();
        reg = clockRegs[ptr];
        Symbol s = regMap.get(reg);
        if ((!(s instanceof FuncFormParam) || s.getDim() == 0) && s.getBlockLevel() != 0) {
            MipsGenerator.mipsCodeList.add("#MEM" + " <--- " + s.getName() + ": " + reg);
            MipsGenerator.pushBackOrLoadFromMem(reg, s, 0, Instruction.LS.Op.sw);
        }
        if (mp) regMap.put(reg, sym);
        return reg;
    }

    // ------- graph regs -------

    public boolean inGraph(Symbol sym) {
        return graphMap.containsKey(sym);
    }

    public HashMap<Symbol, Boolean> getGraphVar() {
        HashMap<Symbol, Boolean> ret = new HashMap<>();
        for (String reg : regMap.keySet()) {
            Symbol s = regMap.get(reg);
            if (s != null && inGraph(s)) ret.put(s, dirtyRegs.get(reg));
        }
        return ret;
    }


    // ------- tot regs -------

    public void saveToMem(String reg) {
        dirtyRegs.put(reg, false);
        MipsGenerator.mipsCodeList.add("#MEM" + " <--- " + regMap.get(reg).getName() + ": " + reg);
        MipsGenerator.pushBackOrLoadFromMem(reg, regMap.get(reg), 0, Instruction.LS.Op.sw);
    }

    public String find(Symbol sym) {
        for (String reg : Regs) {
            if (regMap.get(reg) == sym) return reg;
        }
        if (graphMap.containsKey(sym)) {
            dirtyRegs.put(graphMap.get(sym), false);
            regMap.put(graphMap.get(sym), sym);
            return graphMap.get(sym);
        }
        return null;
    }

    public boolean isDirty(String reg) {
        return dirtyRegs.get(reg);
    }

    public void setDirty(String reg) {
        dirtyRegs.put(reg, true);
    }

    public HashMap<String, Symbol> getAllUsed() {
        HashMap<String, Symbol> used = new HashMap<>();
        for (String reg : Regs) {
            if (regMap.get(reg) != null) used.put(reg, regMap.get(reg));
        }
        return used;
    }

    public void refresh() {
        for (String reg : Regs) {
            dirtyRegs.put(reg, false);
            regMap.put(reg, null);
        }
    }

    public void refreshOne(String reg) {
        dirtyRegs.put(reg, false);
        regMap.put(reg, null);
    }

    public String alloc(Symbol sym, boolean force, String conflict, boolean mp) {
        if (graphMap.containsKey(sym)) {
            if (mp) regMap.put(graphMap.get(sym), sym);
            MipsGenerator.mipsCodeList.add("#" + graphMap.get(sym) + " <--- " + sym.getName() + " (graph)");
            return graphMap.get(sym);
        }
        if (!force) return clkAllocFreeOne(sym, conflict, mp);
        else return clkMandatoryAlloc(sym, conflict, mp);
    }

    public void map(String reg, Symbol sym) {
        regMap.put(reg, sym);
    }


    public Symbol get(String reg) {
        return regMap.get(reg);
    }

    public void recover(HashMap<Symbol, Boolean> x, HashMap<Symbol, String> y) {
        for (Symbol s : x.keySet()) {
            boolean dirty = x.get(s);
            String reg = y.get(s);
            regMap.put(reg, s);
            dirtyRegs.put(reg, dirty);
        }
    }
}
