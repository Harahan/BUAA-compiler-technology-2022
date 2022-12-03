package Middle.Util;

import Middle.Visitor;
import Symbol.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Code {
    public enum Op {
        // ALU
        ASSIGN("="), // ASSIGN VAR (EMPTY) VAR
        ADD("+"), // ADD VAR VAR VAR
        SUB("-"), // SUB VAR VAR VAR
        MUL("*"), // MUL VAR VAR VAR
        DIV("/"), // DIV VAR VAR VAR
        MOD("%"), // MOD VAR VAR VAR
        NOT("!"), // NOT VAR (EMPTY) VAR
        EQ("=="), // EQ VAR VAR VAR
        NE("!="), // NE VAR VAR VAR
        GT(">"), // GT VAR VAR VAR
        GE(">="), // GE VAR VAR VAR
        LT("<"), // LT VAR VAR VAR
        LE("<="), // LE VAR VAR VAR

        // IO
        GET_INT("get_int"), // GET_INT (EMPTY) (EMPTY) VAR or (EMPTY)
        PRINT_STR("print_str"), // PRINT_STR STR (EMPTY) (EMPTY)
        PRINT_INT("print_int"), // PRINT_INT VAR (EMPTY) (EMPTY)

        // FUNC
        FUNC("func"), //FUNC VAR (EMPTY) (EMPTY)
        FUNC_END("func_end"), //FUNC_END VAR (EMPTY) (EMPTY)
        PREPARE_CALL("prepare_call"), // PREPARE_CALL VAR (EMPTY) (EMPTY)
        CALL("call"), //FUNC VAR (EMPTY) (EMPTY)
        PUSH_PAR_INT("push_para_int"), // PUSH_PAR_INT VAR (EMPTY) (EMPTY)
        PUSH_PAR_ADDR("push_para_addr"), // PUSH_PAR_ADDR VAR (EMPTY) (EMPTY)
        RETURN("return"), // RETURN [VAR or (EMPTY)] (EMPTY) (EMPTY)

        // JUMP
        JUMP("jump"), // JUMP (LABEL) (EMPTY) (EMPTY)
        NEZ_JUMP("nez_jump"), // NEZ_JUMP (VAR) (EMPTY) (LABEL)
        EQZ_JUMP("eqz_jump"), // EQZ_JUMP (VAR) (EMPTY) (LABEL)
        LABEL("label"), // LABEL [(AUTO) or (LABEL)] (EMPTY) (EMPTY)

        // DEF
        DEF_VAL("var_def"), // DEF_VAL [VAR or (EMPTY)] (EMPTY) VAR
        DEF_ARR("arr_def"), // DEF_ARR (EMPTY) (EMPTY) VAR
        END_DEF_ARR("end_arr_def"), // END_DEF_ARR (EMPTY) (EMPTY) VAR

        // BLOCK
        BLOCK_BEGIN("block_begin"), // BLOCK_BEGIN VAR (EMPTY) (EMPTY)
        BLOCK_END("block_end"), // BLOCK_END VAR (EMPTY) (EMPTY)

        NOP("nop"); // NOP (EMPTY) (EMPTY) (EMPTY)

        private String op;

        Op(String op) {
            this.op = op;
        }
    }
    private Op instr;
    private String ord1;
    private String ord2;
    private String res;
    private Symbol symbolOrd1 = null;
    private Symbol symbolOrd2 = null;
    private Symbol symbolRes = null;

    public static final HashSet<Op> alu = new HashSet<Op>() {{
        add(Op.ASSIGN); add(Op.SUB); add(Op.MUL); add(Op.ADD); add(Op.MOD); add(Op.NOT); add(Op.DIV);
        add(Op.EQ); add(Op.GE); add(Op.GT); add(Op.LT); add(Op.LE); add(Op.NE);
    }};
    public static final HashSet<Op> io = new HashSet<Op>() {{
        add(Op.GET_INT); add(Op.PRINT_STR); add(Op.PRINT_INT);
    }};
    public static final HashSet<Op> func = new HashSet<Op>() {{
        add(Op.FUNC); add(Op.FUNC_END); add(Op.CALL); add(Op.PREPARE_CALL); add(Op.PUSH_PAR_ADDR); add(Op.PUSH_PAR_INT);
        add(Op.RETURN);
    }};
    public static final HashSet<Op> def = new HashSet<Op>() {{
        add(Op.DEF_ARR); add(Op.DEF_VAL); add(Op.END_DEF_ARR);
    }};
    public static final HashSet<Op> block = new HashSet<Op>() {{
        add(Op.BLOCK_BEGIN); add(Op.BLOCK_END);
    }};
    public static final HashSet<Op> jump = new HashSet<Op>() {{
        add(Op.JUMP); add(Op.NEZ_JUMP); add(Op.EQZ_JUMP); add(Op.LABEL);
    }};

    public static final Pattern varPattern = Pattern.compile("^(.+?\\(\\d+,\\d+\\)).*$");
    public static final Pattern digitPattern = Pattern.compile("^\\d+$");
    public static final Pattern tempVarPattern = Pattern.compile("\\(T(\\d+)\\)");
    public static final Pattern indexPattern = Pattern.compile("^.+\\[(.+)]$");
    public static final Pattern label = Pattern.compile("^\\(LABEL\\d+\\)$");

    public Code(Op instr, String ord1, String ord2, String res) {
        if ("-0123456789".indexOf(ord1.charAt(0)) != -1 && (instr == Op.ADD || instr == Op.MUL || instr == Op.EQ || instr == Op.NE)) {
            // System.out.println("Error: " + instr + " " + ord1 + " " + ord2 + " " + res);
            this.instr = instr;
            this.ord1 = ord2;
            this.ord2 = ord1;
            this.res = res;
        } else {
            this.instr = instr;
            this.ord1 = ord1;
            this.ord2 = ord2;
            this.res = res;
        }
        Matcher matcherOrd1 = varPattern.matcher(this.ord1);
        Matcher matcherOrd2 = varPattern.matcher(this.ord2);
        Matcher matcherRes = varPattern.matcher(this.res);
        if (matcherOrd1.matches()) symbolOrd1 = Visitor.str2Symbol.getOrDefault(matcherOrd1.group(1), null);
        if (matcherOrd2.matches()) symbolOrd2 = Visitor.str2Symbol.getOrDefault(matcherOrd2.group(1), null);
        if (matcherRes.matches()) symbolRes = Visitor.str2Symbol.getOrDefault(matcherRes.group(1), null);
        matcherOrd1 = tempVarPattern.matcher(this.ord1);
        matcherOrd2 = tempVarPattern.matcher(this.ord2);
        matcherRes = tempVarPattern.matcher(this.res);
        if (matcherOrd1.matches()) symbolOrd1 = Visitor.str2Symbol.getOrDefault(matcherOrd1.group(0), null);
        if (matcherOrd2.matches()) symbolOrd2 = Visitor.str2Symbol.getOrDefault(matcherOrd2.group(0), null);
        if (matcherRes.matches()) symbolRes = Visitor.str2Symbol.getOrDefault(matcherRes.group(0), null);
        // if (res.equals("(T4)")) System.out.println(res + " " + symbolRes);
        // System.out.println(this.instr + " " + symbolOrd1 + " " + symbolOrd2 + " " + symbolRes);
    }

    @Override
    public String toString() {
        if (alu.contains(instr)) {
            // VAR (EMPTY) VAR
            if (instr == Op.ASSIGN || instr == Op.NOT) return instr + " " + ord1 + " " + res;
            // VAR VAR VAR
            return instr + " " + ord1 + " " + ord2 + " " + res;
        }
        if (block.contains(instr)) {
            return "--------" + instr + ": " + ord1 + "-------";
        }
        if (def.contains(instr)) {
            if (instr == Op.DEF_ARR) return instr + " " + res;
            else if (instr == Op.DEF_VAL && !Objects.equals(ord1, "(EMPTY)")) return instr + " " + ord1 + " " + res;
            else if (instr == Op.DEF_VAL && Objects.equals(ord1, "(EMPTY)")) return instr + " " + res;
            else if (instr == Op.END_DEF_ARR) return instr + " " + res;
        }
        if (io.contains(instr)) {
            if (instr == Op.GET_INT) return instr + " " + res;
            else return instr + " " + ord1;
        }
        if (func.contains(instr)) {
            if (instr == Op.PUSH_PAR_ADDR || instr == Op.PUSH_PAR_INT
                || instr == Op.PREPARE_CALL || instr == Op.CALL) return instr + " " + ord1;
            else if (instr == Op.FUNC) return "======" + instr + ": " + ord1 + "======";
            else if (instr == Op.FUNC_END) return "======" + instr + ": " + ord1 + "======";
            else if (instr == Op.RETURN && Objects.equals(ord1, "(EMPTY)")) return instr.toString();
            else if (instr == Op.RETURN) return instr + " " + ord1;
        }
        if (jump.contains(instr)) {
            if (instr == Op.LABEL) return ord1 + ":";
            else if (instr == Op.JUMP) return instr + " " + ord1;
            else if (instr == Op.EQZ_JUMP || instr == Op.NEZ_JUMP) return instr + " " + ord1 + " " + res;
        }
        if (instr == Op.NOP) return "NOP";
        return null;
    }

    public Op getInstr() {
        return instr;
    }

    public Symbol getSymbolOrd1() {
        return symbolOrd1;
    }

    public Symbol getSymbolOrd2() {
        return symbolOrd2;
    }

    public Symbol getSymbolRes() {
        return symbolRes;
    }

    public String getOrd1() {
        return ord1;
    }

    public String getRes() {
        return res;
    }

    public String getOrd2() {
        return ord2;
    }

    public void clearRes(String x) {
        if (x != null) {
            res = x;
            Matcher matcherRes = varPattern.matcher(res);
            if (matcherRes.matches()) symbolRes = Visitor.str2Symbol.getOrDefault(matcherRes.group(1), null);
            matcherRes = tempVarPattern.matcher(res);
            if (matcherRes.matches()) symbolRes = Visitor.str2Symbol.getOrDefault(matcherRes.group(0), null);
            return;
        }
        this.res = "(EMPTY)";
        symbolRes = null;
    }

    public void clearOrd1(String x) {
        if (x != null) {
            ord1 = x;
            Matcher matcherOrd1 = varPattern.matcher(ord1);
            if (matcherOrd1.matches()) symbolOrd1 = Visitor.str2Symbol.getOrDefault(matcherOrd1.group(1), null);
            matcherOrd1 = tempVarPattern.matcher(ord1);
            if (matcherOrd1.matches()) symbolOrd1 = Visitor.str2Symbol.getOrDefault(matcherOrd1.group(0), null);
            return;
        }
        this.ord1 = "(EMPTY)";
        symbolOrd1 = null;
    }

    // clearOrd2
    public void clearOrd2(String x) {
        if (x != null) {
            ord2 = x;
            Matcher matcherOrd2 = varPattern.matcher(ord2);
            if (matcherOrd2.matches()) symbolOrd2 = Visitor.str2Symbol.getOrDefault(matcherOrd2.group(1), null);
            matcherOrd2 = tempVarPattern.matcher(ord2);
            if (matcherOrd2.matches()) symbolOrd2 = Visitor.str2Symbol.getOrDefault(matcherOrd2.group(0), null);
            return;
        }
        this.ord2 = "(EMPTY)";
        symbolOrd2 = null;
    }

    // setOp
    public void setOp(Op op) {
        this.instr = op;
    }

    public Symbol getDef() {
        // array or call are not take into consideration
        if ((alu.contains(instr) || instr == Op.DEF_VAL || instr == Op.GET_INT) && symbolRes != null && symbolRes.getDim() == 0) return symbolRes;
        return null;
    }

    public HashMap<Symbol, Integer> getUse() {
        // array, call and return value are not take into consideration
        HashMap<Symbol, Integer> use = new HashMap<Symbol, Integer>();
        if (alu.contains(instr) || io.contains(instr) || jump.contains(instr) || def.contains(instr)) {
            if (symbolOrd1 != null && symbolOrd1.getDim() == 0) use.put(symbolOrd1, 0);
            if (symbolOrd2 != null && symbolOrd2.getDim() == 0) use.put(symbolOrd2, 1);
        }
        if ((instr == Op.PUSH_PAR_INT || instr == Op.RETURN) && symbolOrd1 != null && symbolOrd1.getDim() == 0) use.put(symbolOrd1, 0);
        // 注意数组的索引
        Matcher matcherRes = indexPattern.matcher(res);
        Matcher matcherOrd1 = indexPattern.matcher(ord1);
        Matcher matcherOrd2 = indexPattern.matcher(ord2);
        if (matcherRes.matches() && Visitor.str2Symbol.containsKey(matcherRes.group(1))) use.put(Visitor.str2Symbol.get(matcherRes.group(1)), 4);
        if (matcherOrd1.matches() && Visitor.str2Symbol.containsKey(matcherOrd1.group(1))) use.put(Visitor.str2Symbol.get(matcherOrd1.group(1)), 2);
        if (matcherOrd2.matches() && Visitor.str2Symbol.containsKey(matcherOrd2.group(1))) use.put(Visitor.str2Symbol.get(matcherOrd2.group(1)), 3);
        // System.out.println("use: " + use);
        return use;
    }

    public Symbol getGen() {
        // array, call and return value are not take into consideration
        if (alu.contains(instr) || io.contains(instr) || def.contains(instr)) {
            if (symbolRes != null && symbolRes.getDim() == 0) return symbolRes;
        }
        return null;
    }

    public void reSet(Integer op, String ord, Symbol symbol) {
        if (op == 0) {
            ord1 = ord;
            symbolOrd1 = symbol;
        } else if (op == 1) {
            ord2 = ord;
            symbolOrd2 = symbol;
        } else if (op == 2) {
            // System.out.println(ord1);
            ord1 = ord1.replaceFirst("\\[.*]", "[" + ord + "]");
            // System.out.println(ord1);
        } else if (op == 3) {
            ord2 = ord2.replaceFirst("\\[.*]", "[" + ord + "]");
        } else if (op == 4) {
            res = res.replaceFirst("\\[.*]", "[" + ord + "]");
        }
    }
}
