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
                while ((codes.get(j).startsWith("LABEL") || codes.get(j).startsWith("#") || codes.get(j).startsWith("\n")) && !codes.get(j).startsWith(tokens[1])) ++j;
                // not LABEL or target
                if (codes.get(j).startsWith(tokens[1])) {
                    codes.remove(i);
                    --i;
                }
            } else if  (code.startsWith("bnez ") || code.startsWith("beqz ")) {
                String[] tokens = code.split(" ");
                // System.out.println(tokens[2]);
                int j = i + 1;
                while ((codes.get(j).startsWith("LABEL") || codes.get(j).startsWith("#") || codes.get(j).startsWith("\n")) && !codes.get(j).startsWith(tokens[2])) ++j;
                // not LABEL or target
                if (codes.get(j).startsWith(tokens[2])) {
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
                while ((codes.get(j).startsWith("#") || codes.get(j).startsWith("\n"))) ++j;
                // not LABEL or target
                if (codes.get(j).startsWith("lw ")  && codes.get(j).split(" ")[1].startsWith(tokens[1]) && codes.get(j).split(" ")[2].equals(tokens[2])) {
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
                while ((codes.get(j).startsWith("#") || codes.get(j).startsWith("\n"))) ++j;
                // not LABEL or target
                if (codes.get(j).startsWith("sw ") && codes.get(j).split(" ")[1].startsWith(tokens[1]) && codes.get(j).split(" ")[2].equals(tokens[2])) {
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
    }
}
