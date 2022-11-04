package Backend.Util;

import Backend.MipsGenerator;
import Symbol.Symbol;
import Symbol.FuncFormParam;
import Symbol.Num;
import java.util.HashMap;

public class RegAlloc {
    public static final HashMap<String, Pair<Symbol, Integer>> regMap = new HashMap<String, Pair<Symbol, Integer>>() {{
        // $zero -- 0寄存器， $at -- 操作系统使用，
        /* $v0 -- 系统调用， 函数返回值*/put("$v1", null);
        /* $a0 -- 赋值立即数，系统调用， $a1 -- 移位,比较*/
        put("$a2", null); put("$a3", null);
       put("$t0", null); put("$t1", null);
       put("$t2", null); put("$t3", null);
       put("$t4", null); put("$t5", null);
       put("$t6", null); put("$t7", null);
       put("$s0", null); put("$s1", null);
       put("$s2", null); put("$s3", null);
       put("$s4", null); put("$s5", null);
       put("$s6", null); put("$s7", null);
       put("$t8", null); put("$t9", null);
       put("$k0", null); put("$k1", null);
        //  $gp -- 全局int变量指针，$sp -- 栈指针，
       put("$fp", null); // $ra -- 返回值
    }};

    private static final String[] availableRegs = new String[] {
            /*"$zero", "$at", "$v0",*/ "$v1",
            /*"$a0", $a1*/"$a2", "$a3",
            "$t0", "$t1", "$t2", "$t3",
            "$t4", "$t5", "$t6", "$t7",
            "$s0", "$s1", "$s2", "$s3",
            "$s4", "$s5", "$s6", "$s7",
            "$t8", "$t9", "$k0", "$k1",
            /*"$gp", "$sp",*/ "$fp", /*"$ra"*/
    };

    public static final HashMap<String, Integer> totRegs = new HashMap<String, Integer>() {{
        put("$zero", 0); put("$at", 1);
        put("$v0", 2); put("$v1", 3);
        put("$a0", 4); put("$a1", 5);
        put("$a2", 6); put("$a3", 7);
        put("$t0", 8); put("$t1", 9);
        put("$t2", 10); put("$t3", 11);
        put("$t4", 12); put("$t5", 13);
        put("$t6", 14); put("$t7", 15);
        put("$s0", 16); put("$s1", 17);
        put("$s2", 18); put("$s3", 19);
        put("$s4", 20); put("$s5", 21);
        put("$s6", 22); put("$s7", 23);
        put("$t8", 24); put("$t9", 25);
        put("$k0", 26); put("$k1", 27);
        put("$gp", 28); put("$sp", 29);
        put("$fp", 30); put("$ra", 31);
    }};


    private static int ptr = -1;

    /**
     * 如果是形参且为地址那么不用回写，因为一定为常数，否则会将地址写到地址处
     */
    public static String mandatoryAllocOne(Symbol sym, Integer off, boolean set) {
        ++ptr;
        if (ptr == availableRegs.length) ptr = 0;
        // if (availableRegs[ptr].equals(o)) ++ptr;
        String reg = availableRegs[ptr];
        Pair<Symbol, Integer> p = regMap.get(reg);
        if (p != null && !(p.getKey() instanceof Num)) {
            regMap.put(reg, null);
            if (!(p.getKey() instanceof FuncFormParam) || p.getKey().getDim() == 0) MipsGenerator.pushBackOrLoadFromMem(reg, p.getKey(), p.getValue(), Instruction.LS.Op.sw);
        }
        if (set) {
            regMap.put(reg, new Pair<>(sym, off));
            /*if (sym.getNickname().equals("i(1,7)"))*/ MipsGenerator.mipsCodeList.add("# " + reg + " <--- " + sym.getNickname());
        }
        return reg;
    }

    public static String mandatoryAllocOne(String o, Symbol sym, Integer off, boolean set) {
        int nxt = ((ptr == availableRegs.length - 1) ? 0 : (ptr + 1));
        if (availableRegs[nxt].equals(o)) ptr = nxt;
        return mandatoryAllocOne(sym, off, set);
    }

    public static void mandatorySet(String reg, Symbol sym, Integer off) {
        /*if (sym.getNickname().equals("i(1,7)"))*/ MipsGenerator.mipsCodeList.add("# " + reg + " <--- " + sym.getNickname());
        regMap.put(reg, new Pair<>(sym, off));
    }

    public static void allocFreeOne(Symbol sym, Integer off) {
        ++ptr;
        if (ptr == availableRegs.length) ptr = 0;
        String reg = availableRegs[ptr];
        Pair<Symbol, Integer> p = regMap.get(reg);
        if (p != null) return;
        regMap.put(reg, new Pair<>(sym, off));
    }

    public static String find(Symbol sym, Integer off) {
        for (String reg : availableRegs) {
            Pair<Symbol, Integer> p = regMap.get(reg);
            // System.out.println(p);
            if (p != null && p.getKey().equals(sym) && p.getValue().equals(off)) {
                /*if (sym.getNickname().equals("i(1,7)"))*/ MipsGenerator.mipsCodeList.add("# " + reg + " <--- " + sym.getNickname());
                return reg;
            }
        }
        return null;
    }

    public static HashMap<String, Pair<Symbol, Integer>> getAllUsed() {
        HashMap<String, Pair<Symbol, Integer>> used = new HashMap<>();
        for (String reg : availableRegs) {
            Pair<Symbol, Integer> p = regMap.get(reg);
            if (p != null) used.put(reg, p);
        }
        return used;
    }

    public static void refreshOne(String reg) {
        Pair<Symbol, Integer> p = regMap.get(reg);
        regMap.put(reg, null);
    }

    public static void reflectOne(String reg, Pair<Symbol, Integer> p) {
        // System.out.println(reg + " " + p);
        regMap.put(reg, p);
    }

    static public class Pair<K, T> {
        private K k;
        private T t;
        public Pair(K k, T t) {
            this.t = t;
            this.k = k;
        }

        public K getKey() {
            return k;
        }

        public T getValue() {
            return t;
        }
    }
}
