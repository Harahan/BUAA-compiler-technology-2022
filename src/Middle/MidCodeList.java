package Middle;

import Middle.Util.Code;
import Symbol.Symbol;
import Symbol.Tmp;
import Symbol.Func;

import java.util.ArrayList;
import java.util.Objects;

public class MidCodeList {
    public static ArrayList<Code> codes = new ArrayList<>();
    public static ArrayList<String> strings = new ArrayList<>();
    static int varCounter = -1;
    public static int labelCounter = -1;
    public static String add(Code.Op instr, String ord1, String ord2, String res) {
        // auto var
        if (Objects.equals(res, "(AUTO)")) {
            res = "(T" + ++varCounter + ")";
            Visitor.curTable.add(new Tmp("(T" + varCounter + ")", Visitor.curTable));
        }
        // str
        if (instr == Code.Op.PRINT_STR) {
            strings.add(ord1);
            ord1 = "(STR" + (strings.size() - 1) + ")";
        }
        // label
        if ((instr == Code.Op.JUMP || instr == Code.Op.LABEL) && ord1.equals("(AUTO)")) ord1 = "(LABEL" + ++labelCounter + ")";
        // else if (instr == Code.Op.LABEL) ord1 = ord1.substring(0, 6) + "_END" + ord1.substring(6); // end label
        codes.add(new Code(instr, ord1, ord2, res));

        // call
        if (instr == Code.Op.CALL) {
            res = "(RT)";
            if (Visitor.str2Symbol.containsKey(ord1)) {
                Symbol sym =  Visitor.str2Symbol.get(ord1);
                if (sym instanceof Func && ((Func) sym).getType() == Func.Type.intFunc) {
                    res = "(T" + ++varCounter + ")";
                    Visitor.curTable.add(new Tmp("(T" + varCounter + ")", Visitor.curTable));
                    codes.add(new Code(Code.Op.ASSIGN, "(RT)", "(EMPTY)", "(T" + varCounter + ")"));
                }
            }
        }
        // label
        if (instr == Code.Op.LABEL || instr == Code.Op.JUMP) res = ord1;
        return res;
    }

    public static String printMidCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.size(); ++i) sb.append("(STR").append(i).append(") ").append(strings.get(i)).append("\n");
        for (Code code : codes) sb.append(code).append("\n");
        return sb.toString();
     }
}
