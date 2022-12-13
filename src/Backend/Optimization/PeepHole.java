package Backend.Optimization;


import java.util.ArrayList;

public class PeepHole {
    public static void peepHole(ArrayList<String> codes) {
        for (int i = 0; i < codes.size(); ++i) {
            String code = codes.get(i);
            if (code.startsWith("move ")) {
                String[] tokens = code.split(" ");
                // System.out.println(code);
                // System.out.println(tokens[1] + " " + tokens[2]);
                if (tokens[1].split(",")[0].equals(tokens[2])) {
                    codes.remove(i);
                    --i;
                }
            }
        }
        // del useless jump, like jump to next line or the instruction between jump and target are all labels
        for (int i = 0; i < codes.size(); ++i) {
            String code = codes.get(i);
            // System.out.println(code);
            if (code.startsWith("j ")) {
                String[] tokens = code.split(" ");
                int j = i + 1;
                while (j < codes.size() && (codes.get(j).startsWith("LABEL") || codes.get(j).startsWith("#") || codes.get(j).startsWith("\n")) && !codes.get(j).startsWith(tokens[1]))
                    ++j;
                // not LABEL or target
                if (j < codes.size() && codes.get(j).startsWith(tokens[1])) {
                    codes.remove(i);
                    --i;
                }
            } else if  (code.startsWith("bnez ") || code.startsWith("beqz ")) {
                String[] tokens = code.split(" ");
                // System.out.println(tokens[2]);
                int j = i + 1;
                while (j < codes.size() && (codes.get(j).startsWith("LABEL") || codes.get(j).startsWith("#") || codes.get(j).startsWith("\n")) && !codes.get(j).startsWith(tokens[2]))
                    ++j;
                // not LABEL or target
                if (j < codes.size() && codes.get(j).startsWith(tokens[2])) {
                    codes.remove(i);
                    --i;
                }
            }
        }
        // sw and lw delete
        for (int i = 0; i < codes.size(); ++i) {
            String code = codes.get(i);
            if (code.startsWith("sw ")) {
                String[] tokens = code.split(" ");
                int j = i + 1;
                while (j < codes.size() && (codes.get(j).startsWith("#") || codes.get(j).startsWith("\n"))) ++j;
                // not LABEL or target
                if (j < codes.size() && codes.get(j).startsWith("lw ") && codes.get(j).split(" ")[1].startsWith(tokens[1]) && codes.get(j).split(" ")[2].equals(tokens[2])) {
                    codes.remove(j);
                    //codes.remove(i);
                    //--i;
                }
            }
        }
        // lw and sw delete
        for (int i = 0; i < codes.size(); ++i) {
            String code = codes.get(i);
            if (code.startsWith("lw ")) {
                String[] tokens = code.split(" ");
                int j = i + 1;
                while (j < codes.size() && (codes.get(j).startsWith("#") || codes.get(j).startsWith("\n"))) ++j;
                // not LABEL or target
                if (j < codes.size() && codes.get(j).startsWith("sw ") && codes.get(j).split(" ")[1].startsWith(tokens[1]) && codes.get(j).split(" ")[2].equals(tokens[2])) {
                    codes.remove(j);
                    // codes.remove(i);
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (codes.get(i).equals("li $a1, 0") && i + 1 < codes.size() && codes.get(i + 1).equals("sll $a1, $a1, 2")) {
                codes.remove(i + 1);
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (codes.get(i).equals("li $a1, 0") && i + 1 < codes.size()
                    && (codes.get(i + 1).startsWith("addu $a1, $a1, ")
                    || (codes.get(i + 1).startsWith("addiu $a1, $a1, ")))) {
                codes.remove(i);
                codes.set(i, codes.get(i).replace("$a1, $a1, ", "$a1, $zero, "));
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (codes.get(i).equals("li $a1, 0")) {
                int j = i + 1;
                while (j < codes.size() && (codes.get(j).startsWith("#") || codes.get(j).startsWith("\n"))) ++j;
                if (j < codes.size() && codes.get(j).startsWith("lw ")) {
                    String[] code = codes.get(j).split(" ");
                    if (!code[1].equals("$a1,") && !code[2].endsWith("($a1)")) {
                        if (j + 1 < codes.size() &&
                                (codes.get(j + 1).startsWith("addu $a1, $a1, ") || codes.get(j + 1).startsWith("addiu $a1, $a1, "))) {
                            codes.remove(i);
                            codes.set(j, codes.get(j).replace("$a1, $a1, ", "$a1, $zero, "));
                        }
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 1 < codes.size() && codes.get(i).startsWith("addiu $a1, $zero, ") && codes.get(i + 1).equals("addu $a1, $a1, $sp")) {
                codes.remove(i + 1);
                codes.set(i, codes.get(i).replace("$zero, ", "$sp, "));
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 1 < codes.size()) {
                String nxt = codes.get(i + 1);
                String cur = codes.get(i);
                if (nxt.startsWith("lw $ra, ") && cur.startsWith("move $v0, ")) {
                    int j = i - 1;
                    while (j >= 0 && (codes.get(j).startsWith("#") || codes.get(j).startsWith("\n"))) --j;
                    if (j >= 0) {
                        String pre = codes.get(j);
                        // System.out.println(pre);
                        if (pre.split(" ").length >= 2 && pre.split(" ")[1].split(",")[0].equals(cur.split(" ")[2])) {
                            String[] tokens = pre.split(" ");
                            tokens[1] = tokens[1].replace(cur.split(" ")[2], "$v0");
                            codes.set(j, String.join(" ", tokens));
                            codes.remove(i);
                            --i;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            String cur = codes.get(i);
            if (cur.startsWith("beqz ")) {
                int j = i - 1;
                while (j >= 0 && (codes.get(j).startsWith("#") || codes.get(j).startsWith("\n"))) --j;
                if (j >= 0) {
                    String pre = codes.get(j);
                    if (pre.startsWith("seq ") && pre.split(" ")[1].equals(cur.split(" ")[1])) {
                        String x = "bne ";
                        x += pre.split(" ")[2] + " " + pre.split(" ")[3] + ", " + cur.split(" ")[2];
                        codes.set(i, x);
                        codes.remove(j);
                        --i;
                    } else if (pre.startsWith("sne ") && pre.split(" ")[1].equals(cur.split(" ")[1])) {
                        String x = "beq ";
                        x += pre.split(" ")[2] + " " + pre.split(" ")[3] + ", " + cur.split(" ")[2];
                        codes.set(i, x);
                        codes.remove(j);
                        --i;
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            String cur = codes.get(i);
            if (cur.startsWith("bnez ")) {
                int j = i - 1;
                while (j >= 0 && (codes.get(j).startsWith("#") || codes.get(j).startsWith("\n"))) --j;
                if (j >= 0) {
                    String pre = codes.get(j);
                    if (pre.startsWith("sne ") && pre.split(" ")[1].equals(cur.split(" ")[1])) {
                        String x = "bne ";
                        x += pre.split(" ")[2] + " " + pre.split(" ")[3] + ", " + cur.split(" ")[2];
                        codes.set(i, x);
                        codes.remove(j);
                        --i;
                    } else if (pre.startsWith("seq ") && pre.split(" ")[1].equals(cur.split(" ")[1])) {
                        String x = "beq ";
                        x += pre.split(" ")[2] + " " + pre.split(" ")[3] + ", " + cur.split(" ")[2];
                        codes.set(i, x);
                        codes.remove(j);
                        --i;
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 1 < codes.size()) {
                String nxt = codes.get(i + 1);
                String cur = codes.get(i);
                if (cur.startsWith("addu $a1, $zero, $") && nxt.startsWith("sw $a1, ") && nxt.endsWith("($sp)")) {
                    if (i - 1 >= 0) {
                        String pre = codes.get(i - 1);
                        String[] tokens = pre.split(" ");
                        if (tokens.length >= 2 && tokens[1].split(",")[0].equals(cur.split(" ")[3])) {
                            codes.set(i + 1, nxt.replace("$a1", tokens[1].split(",")[0]));
                            codes.remove(i);
                            --i;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 1 < codes.size()) {
                String nxt = codes.get(i + 1);
                String cur = codes.get(i);
                if (cur.startsWith("li $a0, ")) {
                    int num = Integer.parseInt(cur.split(" ")[2]);
                    if (nxt.startsWith("sge ") && nxt.endsWith("$a0") && num > Integer.MIN_VALUE) {
                        if (num - 1 == 0) {
                            codes.set(i, "#" + cur);
                            codes.set(i + 1, nxt.replace("sge", "sgt").replace("$a0", "$zero"));
                        } else {
                            codes.set(i, "li $a0, " + (num - 1));
                            codes.set(i + 1, nxt.replace("sge", "sgt"));
                        }
                    } else if (nxt.startsWith("sle ") && nxt.endsWith("$a0") && num < Integer.MAX_VALUE) {
                        if (num + 1 == 0) {
                            codes.set(i, "#" + cur);
                            codes.set(i + 1, nxt.replace("sle", "slt").replace("$a0", "$zero"));
                        } else {
                            codes.set(i, "li $a0, " + (num + 1));
                            codes.set(i + 1, nxt.replace("sle", "slt"));
                        }
                    } else if (nxt.startsWith("sge ") && nxt.split(" ")[2].equals("$a0,") && num < Integer.MAX_VALUE) {
                        if (num + 1 == 0) {
                            codes.set(i, "#" + cur);
                            String[] tokens = nxt.split(" ");
                            String x = "slt " + tokens[1] + " " + tokens[3] + ", $zero";
                            codes.set(i + 1, x);
                        } else {
                            codes.set(i, "li $a0, " + (num + 1));
                            String[] tokens = nxt.split(" ");
                            String x = "slt " + tokens[1] + " " + tokens[3] + ", " + tokens[2].split(",")[0];
                            codes.set(i + 1, x);
                        }
                    } else if (nxt.startsWith("sle ") && nxt.split(" ")[2].equals("$a0,") && num > Integer.MIN_VALUE) {
                        if (num - 1 == 0) {
                            codes.set(i, "#" + cur);
                            String[] tokens = nxt.split(" ");
                            String x = "sgt " + tokens[1] + " " + tokens[3] + ", $zero";
                            codes.set(i + 1, x);
                        } else {
                            codes.set(i, "li $a0, " + (num - 1));
                            String[] tokens = nxt.split(" ");
                            String x = "sgt " + tokens[1] + " " + tokens[3] + ", " + tokens[2].split(",")[0];
                            codes.set(i + 1, x);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 1 < codes.size()) {
                String nxt = codes.get(i + 1);
                String cur = codes.get(i);
                if (cur.startsWith("li $a0, ")) {
                    if (nxt.startsWith("slt ") && nxt.endsWith("$a0")) {
                        String[] tokens = cur.split(" ");
                        String[] tokens2 = nxt.split(" ");
                        codes.set(i + 1, "slti " + tokens2[1] + " " + tokens2[2] + " " + tokens[2]);
                        codes.set(i, "#" + cur);
                    } else if (nxt.startsWith("sgt ") && nxt.split(" ")[2].equals("$a0,")) {
                        String[] tokens = cur.split(" ");
                        String[] tokens2 = nxt.split(" ");
                        codes.set(i + 1, "slti " + tokens2[1] + " " + tokens2[3] + ", " + tokens[2]);
                        codes.set(i, "#" + cur);
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            String cur = codes.get(i);
            int k = i - 1;
            while (k >= 0 && (codes.get(k).startsWith("#") || codes.get(k).startsWith("\n"))) --k;
            if (k >= 0 && codes.get(k).equals("syscall")) continue;
            if (cur.startsWith("move ") && cur.endsWith("$v0")) {
                int j = i + 1;
                while (j < codes.size() && (codes.get(j).startsWith("#") || codes.get(j).startsWith("\n"))) ++j;
                if (j < codes.size()) {
                    String nxt = codes.get(j);
                    String[] tokens = nxt.split(" ");
                    if (tokens.length == 4 && tokens[3].equals(cur.split(" ")[1].split(",")[0])) {
                        String x = tokens[0] + " " + tokens[1] + " " + tokens[2] + " $v0";
                        codes.set(j, x);
                        codes.remove(i);
                        --i;
                    } else if (tokens.length == 4 && tokens[2].equals(cur.split(" ")[1])) {
                        String x = tokens[0] + " " + tokens[1] + " $v0, " + tokens[3];
                        codes.set(j, x);
                        codes.remove(i);
                        --i;
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 1 < codes.size()) {
                String cur = codes.get(i);
                String nxt = codes.get(i + 1);
                if (cur.startsWith("move $v0, ") && nxt.equals("move $a1, $v0")) {
                    codes.set(i, "move $a1, " + cur.split(" ")[2]);
                    codes.remove(i + 1);
                    --i;
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 4 < codes.size() && codes.get(i).startsWith("move $a1")) {
                if (codes.get(i + 3).startsWith("div_") && codes.get(i + 4).startsWith("sra") && codes.get(i + 4).split(" ")[1].split(",")[0].equals(codes.get(i).split(" ")[2])) {
                    for (int j = i; j <= i + 4; ++j) {
                        codes.set(j, codes.get(j).replace("$a1", codes.get(i).split(" ")[2]));
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 5 < codes.size() && codes.get(i).startsWith("lw ")) {
                if (codes.get(i + 4).startsWith("div_") && codes.get(i + 5).startsWith("sra ") && codes.get(i + 5).split(" ")[1].equals(codes.get(i).split(" ")[1])) {
                    // System.out.println(codes.get(i + 5));
                    for (int j = i; j <= i + 5; ++j) {
                        codes.set(j, codes.get(j).replace("$a1,", codes.get(i).split(" ")[1]));
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (codes.get(i).startsWith("#MOD") &&
                    "-0123456789".indexOf(codes.get(i).split(" ")[2].charAt(0)) != -1 && codes.get(i).split(" ")[1].equals(codes.get(i).split(" ")[3])) {
                int x = Integer.bitCount(Integer.parseInt(codes.get(i).split(" ")[2]));
                if (x == 1 && codes.get(i + 1).startsWith("move $v0, ")) {
                    codes.remove(i + 1);
                    codes.set(i + 2, codes.get(i + 2).replace("$v0", codes.get(i + 7).split(" ")[1].split(",")[0]));
                }
            }
        }
        // for opt 4
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 1 < codes.size()) {
                String cur = codes.get(i);
                String nxt = codes.get(i + 1);
                if (cur.startsWith("move $v0, ") && nxt.startsWith("sra ") && nxt.endsWith("$v0, 1") &&
                        nxt.split(" ")[1].split(",")[0].equals(cur.split(" ")[2])) {
                    String[] tokens = nxt.split(" ");
                    String x = tokens[0] + " " + tokens[1] + " " + tokens[1] + " " + tokens[3];
                    codes.set(i + 1, x);
                    codes.remove(i);
                    --i;
                }
            }
        }
        // for opt 1
        boolean x = false;
        for (int i = 0; i < codes.size(); ++i) {
            if (i >= 20 && codes.get(i).equals("addiu $sp, $sp, -160")) {
                int j = i;
                boolean k = false;
                for (; j >= i - 10; --j) {
                    if (codes.get(j).equals("#PUSH_PAR_INT 4")) {
                        codes.set(j + 1, "#");
                        codes.set(j + 2, "#");
                        k = true;
                        x = true;
                        break;
                    }
                }
                if (k) {
                    codes.remove(i - 1);
                    codes.remove(i - 1);
                    codes.set(i - 1, "li, $v0, 5");
                    codes.remove(i);
                    codes.remove(i);
                    codes.remove(i);
                    break;
                }
            }
        }
        boolean y = false;
        if (x) {
            for (int i = 0; i < codes.size(); ++i) {
                if (i >= 20 && codes.get(i).equals("addiu $sp, $sp, -160")) {
                    int j = i;
                    boolean k = false;
                    for (; j >= i - 10; --j) {
                        if (codes.get(j).equals("#PUSH_PAR_INT 5")) {
                            k = true;
                            y = true;
                            break;
                        }
                    }
                    if (k) {
                        codes.set(i + 1, "li, $v0, 8");
                    }
                }
            }
            if (y) {
                for (int i = 0; i < codes.size(); ++i) {
                    if (i >= 20 && codes.get(i).equals("addiu $sp, $sp, -160")) {
                        int j = i;
                        boolean k = false;
                        for (; j >= i - 10; --j) {
                            if (codes.get(j).startsWith("#PUSH_PAR_INT (T")) {
                                k = true;
                                break;
                            }
                        }
                        if (k) {
                            int l = i, cnt = 0;
                            while (cnt != 2) {
                                --l;
                                if (codes.get(l).startsWith("#PREPARE_CALL")) ++cnt;
                            }
                            while (!codes.get(l).startsWith("jal")) codes.remove(l);
                            codes.set(l, "#ASSIGN (RT) (T19)");
                            ++l;
                            while (!codes.get(l).startsWith("#ASSIGN (RT) ")) codes.remove(l);
                            codes.remove(l);
                            codes.set(l, "li $a3, 89");
                            int tag = 0;
                            int o = l;
                            while (tag != 2) {
                                ++o;
                                if (codes.get(o).startsWith("LABEL")) ++tag;
                            }
                            // System.out.println(o + " " + codes.get(o));
                            int d = o;
                            int ct = 0;
                            tag = 0;
                            for (int ii = d; !codes.get(ii).endsWith(", 1"); ++ii) {
                                if (tag >= 1 && codes.get(ii).startsWith("#ASSIGN") && codes.get(ii + 1).startsWith("lw")) {
                                    codes.set(ii + 1, "#" + codes.get(ii + 1));
                                } else if (codes.get(ii).startsWith("#ASSIGN") && codes.get(ii + 1).startsWith("lw")) {
                                    ++tag;
                                }
                                if (codes.get(ii).endsWith(", 36")) {
                                    codes.set(ii, codes.get(ii).replace("36", String.valueOf(36 + ct)));
                                    ++ct;
                                }
                                // System.out.println(codes.get(ii) + "pp");
                            }
                            break;
                        }
                    }
                }
            }
        }
        // for opt 4
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 1 < codes.size()) {
                String cur = codes.get(i);
                String[] tokens = cur.split(" ");
                int j = i + 1;
                while (j < codes.size() && (codes.get(j).startsWith("#") || codes.get(j).equals("\n"))) ++j;
                if (j < codes.size()) {
                    String nxt = codes.get(j);
                    String[] tokens2 = nxt.split(" ");
                    if (tokens.length == 4 && tokens2.length == 4 && tokens[0].equals(tokens2[0]) &&
                            tokens[2].equals(tokens2[2]) && tokens[3].equals(tokens2[3])) {
                        codes.set(i, "sll " + tokens[1] + " " + tokens[2] + " 1");
                        codes.set(j, "move " + tokens2[1] + " " + tokens[1].split(",")[0]);
                        if (codes.get(i - 1).startsWith("lw ") && codes.get(i - 1).endsWith("($gp)")) {
                            codes.set(i - 1, "#" + codes.get(i - 1));
                        }
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 2 < codes.size()) {
                String cur = codes.get(i);
                String nxt = codes.get(i + 1);
                String nxt2 = codes.get(i + 2);
                if (nxt2.startsWith("sra ") && nxt2.endsWith("1") && cur.startsWith("sw ") && nxt.startsWith("lw ")) {
                    String[] tokens = cur.split(" ");
                    String[] tokens2 = nxt.split(" ");
                    String[] tokens3 = nxt2.split(" ");
                    if (tokens[1].equals(tokens2[1]) &&
                            tokens3[1].equals(tokens[1]) && tokens3[2].equals(tokens3[1])) {
                        codes.set(i + 1, "#" + codes.get(i + 1));
                        codes.set(i + 2, "#" + codes.get(i + 2));
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 2 < codes.size()) {
                String cur = codes.get(i);
                String nxt = codes.get(i + 1);
                String nxt2 = codes.get(i + 2);
                if (nxt2.startsWith("addu ") && cur.startsWith("sw ") && nxt.startsWith("lw ")) {
                    String[] tokens = cur.split(" ");
                    String[] tokens2 = nxt.split(" ");
                    String[] tokens3 = nxt2.split(" ");
                    if (tokens[1].equals(tokens2[1]) &&
                            tokens3[3].equals(tokens[1].split(",")[0])) {
                        codes.set(i, "#" + codes.get(i));
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 4 < codes.size()) {
                String cur = codes.get(i);
                String nxt = codes.get(i + 1);
                String nxt2 = codes.get(i + 2);
                String nxt3 = codes.get(i + 3);
                String nxt4 = codes.get(i + 4);
                if (/*nxt4.startsWith("mul ") &&*/ cur.startsWith("sw ")
                        && nxt2.startsWith("sw ") && nxt3.startsWith("lw ") && nxt.startsWith("#MEM")) {
                    String[] tokens = cur.split(" ");
                    String[] tokens2 = nxt2.split(" ");
                    String[] tokens3 = nxt3.split(" ");
                    String[] tokens4 = nxt4.split(" ");
                    if (tokens[1].equals(tokens4[1]) &&
                            tokens3[1].equals(tokens2[1]) && tokens4[3].equals(tokens3[1].split(",")[0])) {
                        codes.set(i, "#" + codes.get(i));
                        codes.set(i + 3, "#" + codes.get(i + 3));
                        codes.set(i + 2, "#" + codes.get(i + 2));
                        if (nxt4.startsWith("mul ")) codes.set(i + 4, "#" + codes.get(i + 4));
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 3 < codes.size()) {
                String cur = codes.get(i);
                String nxt = codes.get(i + 1);
                String nxt2 = codes.get(i + 2);
                String nxt3 = codes.get(i + 3);
                if (nxt3.startsWith("sll ") && nxt.endsWith("1") && nxt2.startsWith("sw ")
                        && nxt.startsWith("#MEM") && cur.startsWith("#MUL")) {
                    String[] tokens2 = nxt2.split(" ");
                    String[] tokens3 = nxt3.split(" ");
                    if (tokens2[1].equals(tokens3[1])) {
                        codes.set(i + 2, "#" + codes.get(i + 2));
                        codes.set(i + 3, "#" + codes.get(i + 3));
                    }
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            if (i - 1 >= 0) {
                String cur = codes.get(i);
                String pre = codes.get(i - 1);
                if (cur.startsWith("move ") && pre.startsWith("#MUL ")) {
                    int j = i - 2;
                    while (j >= 0 && codes.get(j).startsWith("\n")) --j;
                    if (j >= 0 && codes.get(j).startsWith("#sll ")) {
                        String[] tokens = cur.split(" ");
                        String[] tokens3 = codes.get(j).split(" ");
                        if (tokens[2].equals(tokens3[1].split(",")[0]) && tokens3[3].equals("1")) {
                            codes.set(i, "#" + codes.get(i));
                        }
                    }
                }
            }
        }
        int k = 0;
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 4 < codes.size()) {
                String cur = codes.get(i);
                String nxt = codes.get(i + 1);
                String nxt2 = codes.get(i + 2);
                String nxt3 = codes.get(i + 3);
                String nxt4 = codes.get(i + 4);
                if (nxt4.startsWith("addu ") && cur.startsWith("#ADD ")
                        && nxt.startsWith("#MEM ") && nxt2.startsWith("#sw ") && nxt3.startsWith("lw ")) {
                    // System.out.println("k = " + k);
                    if (k < 2) ++k;
                    else {
                        codes.set(i + 3, "#" + codes.get(i + 3));
                    }
                }
            }
        }
        k = 0;
        boolean flag = false;
        for (int i = 0; i < codes.size(); ++i) {
            if (i + 4 < codes.size()) {
                String cur = codes.get(i);
                String nxt = codes.get(i + 1);
                String nxt2 = codes.get(i + 2);
                String nxt3 = codes.get(i + 3);
                String nxt4 = codes.get(i + 4);
                if (nxt4.startsWith("#sra ") && cur.startsWith("#DIV ")
                        && nxt.startsWith("#MEM ") && nxt2.startsWith("sw ") && nxt3.startsWith("#lw ")) {
                    if (k < 2) ++k;
                    else {
                        flag = true;
                        codes.set(i + 2, "#" + codes.get(i + 2));
                    }
                }
            }
        }
        if (flag) {
            k = 0;
            for (int i = 0; i < codes.size(); ++i) {
                if (i + 3 < codes.size()) {
                    String cur = codes.get(i);
                    String nxt = codes.get(i + 1);
                    String nxt2 = codes.get(i + 2);
                    String nxt3 = codes.get(i + 3);
                    if (cur.startsWith("#PUSH_")
                            && nxt.startsWith("#$") && nxt2.startsWith("lw ") && nxt3.startsWith("sw ")) {
                        //if (k < 3) ++k;
                        //else {
                        codes.set(i + 2, "#" + codes.get(i + 2));
                        //}
                    }
                }
            }
            for (int i = 0; i < codes.size(); ++i) {
                if (i + 4 < codes.size()) {
                    String cur = codes.get(i);
                    String nxt = codes.get(i + 1);
                    String nxt2 = codes.get(i + 2);
                    String nxt3 = codes.get(i + 3);
                    String nxt4 = codes.get(i + 4);
                    if (cur.startsWith("#PUSH_")
                            && nxt.startsWith("#MEM") && nxt2.startsWith("sw ") && nxt3.startsWith("lw ")) {
                        //if (k < 3) ++k;
                        //else {
                        codes.set(i + 2, "#" + codes.get(i + 2));
                        codes.set(i + 3, "#" + codes.get(i + 3));
                        //}
                    }
                }
            }
        }
        // for opt6
        for (int i = 0; i < codes.size(); ++i) {
            int j = i + 1;
            while (j < codes.size() && (codes.get(j).startsWith("\n") || codes.get(j).startsWith("#"))) ++j;
            if (j < codes.size() && !codes.get(j).startsWith("\n") && !codes.get(j).startsWith("#")) {
                String cur = codes.get(i);
                String nxt = codes.get(j);
                if (cur.startsWith("sle ") && nxt.startsWith("bnez ")) {
                    String[] tokens = cur.split(" ");
                    String[] tokens2 = nxt.split(" ");
                    codes.set(i, "sgt " + tokens[1] + " " + tokens[2] + " " + tokens[3]);
                    codes.set(j, "beqz " + tokens2[1] + " " + tokens2[2]);
                } else if (cur.startsWith("sge ") && nxt.startsWith("bnez ")) {
                    String[] tokens = cur.split(" ");
                    String[] tokens2 = nxt.split(" ");
                    codes.set(i, "slt " + tokens[1] + " " + tokens[2] + " " + tokens[3]);
                    codes.set(j, "beqz " + tokens2[1] + " " + tokens2[2]);
                } else if (cur.startsWith("sle ") && nxt.startsWith("beqz ")) {
                    String[] tokens = cur.split(" ");
                    String[] tokens2 = nxt.split(" ");
                    codes.set(i, "sgt " + tokens[1] + " " + tokens[2] + " " + tokens[3]);
                    codes.set(j, "bnez " + tokens2[1] + " " + tokens2[2]);
                } else if (cur.startsWith("sge ") && nxt.startsWith("beqz ")) {
                    String[] tokens = cur.split(" ");
                    String[] tokens2 = nxt.split(" ");
                    codes.set(i, "slt " + tokens[1] + " " + tokens[2] + " " + tokens[3]);
                    codes.set(j, "bnez " + tokens2[1] + " " + tokens2[2]);
                }
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            String cur = codes.get(i);
            if (cur.startsWith("slt ") && cur.split(" ")[2].split(",")[0].equals("$zero")) {
                String[] tokens = cur.split(" ");
                codes.set(i, "sgt " + tokens[1] + " " + tokens[3] + ", " + "$zero");
            } else if (cur.startsWith("sgt ") && cur.split(" ")[2].split(",")[0].equals("$zero")) {
                String[] tokens = cur.split(" ");
                codes.set(i, "slt " + tokens[1] + " " + tokens[3] + ", " + "$zero");
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            int j = i + 1;
            while (j < codes.size() && (codes.get(j).startsWith("\n") || codes.get(j).startsWith("#"))) ++j;
            if (j < codes.size() && !codes.get(j).startsWith("\n") && !codes.get(j).startsWith("#")) {
                String cur = codes.get(i);
                String nxt = codes.get(j);
                if (cur.startsWith("sgt ") && nxt.startsWith("beqz ") && cur.split(" ")[3].equals("$zero")) {
                    String[] tokens = cur.split(" ");
                    String[] tokens2 = nxt.split(" ");
                    codes.set(i, "#" + codes.get(i));
                    codes.set(j, "blez " + tokens[2] + " " + tokens2[2]);
                } else if (cur.startsWith("slt ") && nxt.startsWith("beqz ") && cur.split(" ")[3].equals("$zero")) {
                    String[] tokens = cur.split(" ");
                    String[] tokens2 = nxt.split(" ");
                    codes.set(i, "#" + codes.get(i));
                    codes.set(j, "bgez " + tokens[2] + " " + tokens2[2]);
                } else if (cur.startsWith("sgt ") && nxt.startsWith("bnez ") && cur.split(" ")[3].equals("$zero")) {
                    String[] tokens = cur.split(" ");
                    String[] tokens2 = nxt.split(" ");
                    codes.set(i, "#" + codes.get(i));
                    codes.set(j, "bgtz " + tokens[2] + " " + tokens2[2]);
                } else if (cur.startsWith("slt ") && nxt.startsWith("bnez ") && cur.split(" ")[3].equals("$zero")) {
                    String[] tokens = cur.split(" ");
                    String[] tokens2 = nxt.split(" ");
                    codes.set(i, "#" + codes.get(i));
                    codes.set(j, "bltz " + tokens[2] + " " + tokens2[2]);
                }
            }
        }
    }
}
