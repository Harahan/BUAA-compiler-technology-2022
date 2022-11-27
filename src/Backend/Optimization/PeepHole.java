package Backend.Optimization;


import java.util.ArrayList;

public class PeepHole {
    public static void peepHole(ArrayList<String> codes) {
        for (int i = 0; i < codes.size(); ++i) {
            String code = codes.get(i);
            if (code.startsWith("move ")) {
                String[] tokens = code.split(" ");
                // System.out.println(code);
                if (tokens[1].equals(tokens[2])) {
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
    }
}
