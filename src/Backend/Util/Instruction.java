package Backend.Util;

import java.util.ArrayList;

public class Instruction {
    public static String asciiz(String name, String str) {
        return name + ": .asciiz " + "\"" + str + "\"";
    }

    public static String wordOrSpace(String name, ArrayList<Integer> vals, boolean init) {
        StringBuilder rt = new StringBuilder(name);
        if (!init) return rt.append(": .space ").append(vals.size() << 2).toString();
        rt.append(": .word ");
        for (Integer val : vals) rt.append(val).append(" ");
        return rt.toString();
    }

    static public class MMI {
        public enum Op {
            addi, addiu, andi, ori, xori, slti, sltiu, subiu,
            sll, srl, // 逻辑移动
            sra, // 算数右移
        }

        private final String rs;
        private final String rt;
        private final Op op;
        private final Integer imm16;

        public MMI(Op op, String rt, String rs, Integer imm16) {
            this.rt = rt;
            this.rs = rs;
            this.imm16 = imm16;
            this.op = op;
        }

        @Override
        public String toString() {
            return op + " " + rt + ", " + rs + ", " + imm16;
        }
    }

    static public class MMM {
        public enum Op {
            add, addu, sub, subu, slt, sltu, seq, sgt, sne, sge, sle,
            sllv, srlv,
            srav,
            and, or, xor, nor, mul, mulu,
        }

        private final String rs;
        private final String rt;
        private final String rd;
        private final Op op;

        public MMM(Op op, String rd, String rs, String rt) {
            this.rt = rt;
            this.rs = rs;
            this.rd = rd;
            this.op = op;
        }

        @Override
        public String toString() {
            return op + " " + rd + ", " + rs + ", " + rt;
        }
    }

    static public class MM {
        public enum Op {
            mult, multu, div, divu, move, neg,
        }

        private final String rs;
        private final String rt;
        private final Op op;

        public MM(Op op, String rs, String rt) {
            this.rt = rt;
            this.rs = rs;
            this.op = op;
        }

        @Override
        public String toString() {
            return op + " " + rs + ", " + rt;
        }
    }

    static public class MI {
        public enum Op {
            lui, li,
        }

        private final String rt;
        private final Op op;
        private final Integer imm16;

        public MI(Op op, String rt, Integer imm16) {
            this.rt = rt;
            this.imm16 = imm16;
            this.op = op;
        }

        @Override
        public String toString() {
            return op + " " + rt + ", " + imm16;
        }
    }

    static public class ML {
        public enum Op {
            la, bnez, beqz, bgez,
        }

        private final String rt;
        private final Op op;
        private final String label;

        public ML(Op op, String rt, String label) {
            this.rt = rt;
            this.label = label;
            this.op = op;
        }

        @Override
        public String toString() {
            return op + " " + rt + ", " + label;
        }
    }

    static public class LS {
        public enum Op {
            lw, sw,
        }

        private final String rt;
        private final Op op;
        private final Integer off;
        private final String base;
        private final String label;

        public LS(Op op, String rt, String label, Integer off, String base) {
            this.rt = rt;
            this.off = off;
            this.op = op;
            this.label = label;
            this.base = base;
        }

        @Override
        public String toString() {
            if (off >= 0) return op + " " + rt + ", " + (label != null ? label + "+" : "") + off + "(" + base + ")";
            else return op + " " + rt + ", " + (label != null ? label : "") + off + "(" + base + ")";
        }
    }

    static public class NP {
        public enum Op {
            syscall, nop;
        }

        private final Op op;

        public NP(Op op) {
            this.op = op;
        }

        @Override
        public String toString() {
            return op.toString();
        }
    }

    static public class M {
        public enum Op {
            mfhi, mflo, mtlo, mthi, jr
        }

        private final String rd;
        private final Op op;

        public M(Op op, String rd) {
            this.rd = rd;
            this.op = op;
        }

        @Override
        public String toString() {
            return op + " " + rd;
        }
    }

    static public class L {
        public enum Op {
            j,
        }

        private final String label;
        private final Op op;

        public L(Op op, String label) {
            this.label = label;
            this.op = op;
        }

        @Override
        public String toString() {
            return op + " " + label;
        }
    }

}
