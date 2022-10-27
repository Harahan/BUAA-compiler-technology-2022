package Middle;

import Middle.Util.Code;

import java.util.ArrayList;
import java.util.Objects;

public class MidCodeList {
    public static ArrayList<Code> codes = new ArrayList<>();
    public static ArrayList<String> strings = new ArrayList<>();
    static int counter = -1;
    public static String add(Code.Op instr, String ord1, String ord2, String res) {
        // auto var
        if (Objects.equals(res, "(AUTO)")) res = "(T" + ++counter + ")";
        // str
        if (instr == Code.Op.PRINT_STR) {
            strings.add(ord1);
            ord1 = "(STR" + (strings.size() - 1) + ")";
        }
        // call
        if (instr == Code.Op.CALL) res = "(RT)";
        codes.add(new Code(instr, ord1, ord2, res));
        return res;
    }

    public static String printMidCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.size(); ++i) sb.append("(STR").append(i).append(") ").append(strings.get(i)).append("\n");
        for (Code code : codes) sb.append(code).append("\n");
        return sb.toString();
     }
}
