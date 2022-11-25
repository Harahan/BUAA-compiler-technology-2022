package Backend.Optimization;

import Backend.MipsGenerator;
import Backend.Util.Instruction;
import Backend.Util.RegAlloc;
import Symbol.Symbol;
import Symbol.Tmp;

import java.util.ArrayList;
import java.util.Locale;

public class MulDiv {
    public static int N = 32;
    public static int magic = 0;
    public static ArrayList<String> mul(String regRes, String valReg, Integer num, Integer lim) {
        ArrayList<String> rt = new ArrayList<>();
        boolean neg = num < 0;
        int originalNum = num;
        num = Math.abs(num);
        int cnt = 0;
        boolean first = true;
        while (num != 0) {
            if ((num & 1) == 1) {
                if (first) {
                    rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sll, regRes, valReg, cnt)));
                    first = false;
                } else {
                    rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sll, "$a1", valReg, cnt)));
                    rt.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, regRes, regRes, "$a1")));
                }
            }
            num >>= 1;
            cnt += 1;
        }
        if (neg) rt.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.neg, regRes, regRes)));
        if (lim < rt.size()) {
            rt.clear();
            rt.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.mul, regRes, valReg, String.valueOf(originalNum))));
        }
        return rt;
    }


    private static long[] chooseMultiplier(int d, int p) {
        long l = (long) Math.ceil((Math.log(d) / Math.log(2)));
        long sh = l;
        // low 溢出故用 long
        // (((long) 1) << (N + l)) / d 溢出故用
        long low = (long) Math.floor(Math.pow(2, N + l) / d);
        long high = (long) Math.floor((Math.pow(2, N + l) + Math.pow(2, N + l - p)) / d);
        while ((low >> 1) < (high >> 1) && sh > 0) {
            low >>= 1;
            high >>= 1;
            sh -= 1;
        }
        return new long[]{high, sh, l};
    }

    public static ArrayList<String> div(String resReg, String valReg, Integer d, Integer lim, boolean div, Symbol symbol) {
        ArrayList<String> rt = new ArrayList<>();
        long[] multiplier = chooseMultiplier(Math.abs(d), N - 1);
        long l = multiplier[2], sh = multiplier[1], m = multiplier[0];
        int p = Integer.bitCount(d);
        if (p == 1 && div) {
            rt.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, "$a1", valReg)));
            rt.add(String.valueOf(new Instruction.ML(Instruction.ML.Op.bgez, "$a1", "div_" + ++magic)));
            rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, "$a1", "$a1", Math.abs(d) - 1)));
            rt.add("div_" + magic + ":");
            rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sra, resReg, "$a1", Integer.numberOfTrailingZeros(Math.abs(d)))));
        } else if (p == 1) {
            int x = Integer.MIN_VALUE + Math.abs(d) - 1;
            rt.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$a1", x)));
            rt.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.and, resReg, "$a1", valReg)));
            rt.add(String.valueOf(new Instruction.ML(Instruction.ML.Op.bgez, resReg, "div_" + ++magic)));
            rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, resReg, resReg, -1)));
            rt.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$a1", -Math.abs(d))));
            rt.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.or, resReg, resReg, "$a1")));
            rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.addiu, resReg, resReg, 1)));
            rt.add("div_" + magic + ":");
            return rt;
        } else if (Math.abs(d) == 1) {
            rt.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.move, resReg, valReg)));
        } else if (Math.abs(d) == (1 << l)) {
            rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sra, "$a1", valReg, (int) l - 1)));
            rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.srl, "$a1", "$a1", (int) (N - l))));
            rt.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, "$a1", "$a1", valReg)));
            rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sra, resReg, "$a1", (int) l)));
        } else if (m < (1L << (N - 1))) {
            rt.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$a1", (int) m)));
            rt.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.mult, "$a1", valReg)));
            rt.add(String.valueOf(new Instruction.M(Instruction.M.Op.mfhi, "$a1")));
            rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sra, resReg, "$a1", (int) sh)));
            rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sra, "$a1", valReg, 31)));
            rt.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.subu, resReg, resReg, "$a1")));
        } else {
            rt.add(String.valueOf(new Instruction.MI(Instruction.MI.Op.li, "$a1", (int) (m - (1L << N)))));
            rt.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.mult, "$a1", valReg)));
            rt.add(String.valueOf(new Instruction.M(Instruction.M.Op.mfhi, "$a1")));
            rt.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.addu, "$a1", "$a1", valReg)));
            rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sra, resReg, "$a1", (int) sh)));
            rt.add(String.valueOf(new Instruction.MMI(Instruction.MMI.Op.sra, "$a1", valReg, 31)));
            rt.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.subu, resReg, resReg, "$a1")));
        }
        if (d < 0) rt.add(String.valueOf(new Instruction.MM(Instruction.MM.Op.neg, resReg, resReg)));
        if (!div) {
            // a0 not a1
            rt.addAll(mul("$a0", resReg, d, 4));
            //rt.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.mul, "$a1", resReg, String.valueOf(d))));
            rt.add(String.valueOf(new Instruction.MMM(Instruction.MMM.Op.subu, resReg, valReg, "$a0")));
        }
        return rt;
    }
}
