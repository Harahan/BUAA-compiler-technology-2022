package Backend.Optimization;

import Middle.Util.Code;
import Symbol.Func;

import java.util.ArrayList;

public class RedundantCall {
    public static boolean redundantCall(Func func, ArrayList<Code> codes, int i) {
        if (func.callOtherFunc || func.hasIO || func.hasAddressParam || func.useNotConstGlobal) return false;
        // System.out.println("RedundantCall: " + func.getName());
        while (true) {
            Code code = codes.get(i++);
            // System.out.println(code);
            if (code.getInstr() == Code.Op.CALL) break;
            if (code.getSymbolOrd1() != null && code.getSymbolOrd1().getBlockLevel() == 0) return false;
        }
        return !codes.get(i).getOrd1().equals("(RT)");
    }
}
