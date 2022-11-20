package Middle.Optimization;

import Middle.MidCodeList;
import Middle.Util.Code;

import java.util.ArrayList;

public class Block {
    private final ArrayList<Integer> pre = new ArrayList<>();
    private final ArrayList<Integer> nxt = new ArrayList<>();
    private final ArrayList<Code> codes;
    private final int id;

    public Block(Integer id, ArrayList<Code> codes) {
        this.codes = codes;
        this.id = id;
    }

    public ArrayList<Code> getCodes() {
        return codes;
    }

    public ArrayList<Integer> getNxt() {
        return nxt;
    }

    public int getId() {
        return id;
    }

    public ArrayList<Integer> getPre() {
        return pre;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("----------block").append(id).append(" begin----------------\n");
        sb.append("pre: block").append(pre).append(" nxt: block").append(nxt).append("\n");
        for (Code code : codes) sb.append(code).append("\n");
        sb.append("----------block").append(id).append(" end----------------\n");
        return sb.toString();
    }
}
