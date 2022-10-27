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

        // IO
        GET_INT("getInt"), // GET_INT (EMPTY) (EMPTY) VAR
        PRINT_STR("print_str"), // PRINT_STR STR (EMPTY) (EMPTY)
        PRINT_INT("print_int"), // PRINT_INT VAR (EMPTY) (EMPTY)

        // FUNC
        FUNC("func"), //FUNC VAR (EMPTY) (EMPTY)
        FUNC_END("func_end"), //FUNC VAR (EMPTY) (EMPTY)
        PREPARE_CALL("prepare_call"), // PREPARE_CALL VAR (EMPTY) (EMPTY)
        CALL("call"), //FUNC VAR (EMPTY) (EMPTY)
        PUSH_PAR_INT("push_para_int"), // PUSH_PAR_INT VAR (EMPTY) (EMPTY)
        PUSH_PAR_ADDR("push_para_addr"), // PUSH_PAR_ADDR VAR (EMPTY) (EMPTY)
        RETURN("return"), // RETURN [VAR or (EMPTY)] (EMPTY) (EMPTY)

        // JUMP
        JUMP("jump"),
        COND_JUMP("condition_jump"),
        LABEL("label"),

        // DEF
        DEF_VAL("var_def"), // DEF_VAL [VAR or (EMPTY)] (EMPTY) VAR
        DEF_ARR("var_arr"), // DEF_ARR (EMPTY) (EMPTY) VAR

        // BLOCK
        BLOCK_BEGIN("block_begin"), // BLOCK_BEGIN VAR (EMPTY) (EMPTY)
        BLOCK_END("block_end"); // BLOCK_END VAR (EMPTY) (EMPTY)

        private String op;

        Op(String op) {
            this.op = op;
        }
    }
    private final Op instr;
    private final String ord1;
    private final String ord2;
    private final String res;
    private Symbol symbolOrd1 = null;
    private Symbol symbolOrd2 = null;
    private Symbol symbolRes = null;

    private final HashSet<Op> alu = new HashSet<Op>() {{
        add(Op.ASSIGN); add(Op.SUB); add(Op.MUL); add(Op.ADD); add(Op.MOD); add(Op.NOT); add(Op.DIV);
    }};
    private final HashSet<Op> io = new HashSet<Op>() {{
        add(Op.GET_INT); add(Op.PRINT_STR); add(Op.PRINT_INT);
    }};
    private final HashSet<Op> func = new HashSet<Op>() {{
        add(Op.FUNC); add(Op.FUNC_END); add(Op.CALL); add(Op.PREPARE_CALL); add(Op.PUSH_PAR_ADDR); add(Op.PUSH_PAR_INT);
        add(Op.RETURN);
    }};
    private final HashSet<Op> def = new HashSet<Op>() {{
        add(Op.DEF_ARR); add(Op.DEF_VAL);
    }};
    private final HashSet<Op> block = new HashSet<Op>() {{
        add(Op.BLOCK_BEGIN); add(Op.BLOCK_END);
    }};
    private final HashSet<Op> jump = new HashSet<Op>() {{
        add(Op.JUMP); add(Op.COND_JUMP); add(Op.LABEL);
    }};

    private static final Pattern varPattern = Pattern.compile("(.+\\(\\d+,\\d+\\)).*");

    public Code(Op instr, String ord1, String ord2, String res) {
        this.instr = instr;
        this.ord1 = ord1;
        this.ord2 = ord2;
        this.res = res;
        Matcher matcherOrd1 = varPattern.matcher(ord1);
        Matcher matcherOrd2 = varPattern.matcher(ord2);
        Matcher matcherRes = varPattern.matcher(res);
        if (matcherOrd1.matches()) symbolOrd1 = Visitor.str2Symbol.getOrDefault(matcherOrd1.group(1), null);
        if (matcherOrd2.matches()) symbolOrd2 = Visitor.str2Symbol.getOrDefault(matcherOrd2.group(1), null);
        if (matcherRes.matches()) symbolRes = Visitor.str2Symbol.getOrDefault(matcherRes.group(1), null);
        System.out.println(this.instr + " " + symbolOrd1 + " " + symbolOrd2 + " " + symbolRes);
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
        }
        if (io.contains(instr)) {
            if (instr == Op.GET_INT) return instr + " " + res;
            else return instr + " " + ord1;
        }
        if (func.contains(instr)) {
            if (instr == Op.PUSH_PAR_ADDR || instr == Op.PUSH_PAR_INT
                || instr == Op.PREPARE_CALL || instr == Op.CALL) return instr + " " + ord1;
            else if (instr == Op.FUNC) return "\n========" + instr + ": " + ord1 + "========";
            else if (instr == Op.FUNC_END) return "========" + instr + ": " + ord1 + "========\n";
            else if (instr == Op.RETURN && Objects.equals(ord1, "(EMPTY)")) return instr.toString();
            else if (instr == Op.RETURN) return instr + " " + ord1;
        }
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
}
