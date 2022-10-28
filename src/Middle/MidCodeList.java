package Middle;

import Middle.Util.Code;

import java.util.ArrayList;
import java.util.Objects;

public class MidCodeList {
    public static ArrayList<Code> codes = new ArrayList<>();
    public static ArrayList<String> strings = new ArrayList<>();
    static int varCounter = -1;
    public static int labelCounter = -1;
    public static String add(Code.Op instr, String ord1, String ord2, String res) {
        // auto var
        if (Objects.equals(res, "(AUTO)")) res = "(T" + ++varCounter + ")";
        // str
        if (instr == Code.Op.PRINT_STR) {
            strings.add(ord1);
            ord1 = "(STR" + (strings.size() - 1) + ")";
        }
        // label
        if (instr == Code.Op.LABEL && ord1.equals("(AUTO)")) ord1 = "(LABEL" + ++labelCounter + ")";
        // else if (instr == Code.Op.LABEL) ord1 = ord1.substring(0, 6) + "_END" + ord1.substring(6); // end label
        codes.add(new Code(instr, ord1, ord2, res));

        // call
        if (instr == Code.Op.CALL) res = "(RT)";
        // label
        if (instr == Code.Op.LABEL) res = ord1;
        return res;
    }

    public static String printMidCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.size(); ++i) sb.append("(STR").append(i).append(") ").append(strings.get(i)).append("\n");
        for (Code code : codes) sb.append(code).append("\n");
        return sb.toString();
     }
}
