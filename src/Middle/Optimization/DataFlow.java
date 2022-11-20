package Middle.Optimization;

import Middle.Util.Code;
import Middle.Util.Code.Op;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DataFlow {
    private final HashMap<String, ArrayList<Block>> func2blocks = new HashMap<>();
    private final HashMap<String, ArrayList<Code>> func2codes = new HashMap<>();
    public static HashSet<Op> divider = new HashSet<Op>() {{
        // add(Op.LABEL);
        add(Op.JUMP);
        add(Op.NEZ_JUMP);
        add(Op.EQZ_JUMP);
        add(Op.RETURN); // nxt --> -1
        // add(Op.FUNC); // pre --> -1
    }};

    public DataFlow(ArrayList<Code> codes) {
        for (int i = 0; i < codes.size(); ++i) {
            if (codes.get(i).getInstr() == Op.FUNC) {
                String func = codes.get(i).getSymbolOrd1().getNickname();
                ArrayList<Code> funcCodes = new ArrayList<>();
                while (codes.get(i).getInstr() != Op.FUNC_END) funcCodes.add(codes.get(i++));
                // funcCodes.add(codes.get(i)); do not add FUNC_END
                func2codes.put(func, funcCodes);
            }
        }
        for (String func : func2codes.keySet()) divideBlock(func, func2codes.get(func));
    }

    private void divideBlock(String func, ArrayList<Code> codes) {
        // 标记block开始code
        HashSet<Integer> begin = new HashSet<>();
        HashMap<String, Integer> labels = new HashMap<>();
        for (int i = 0; i < codes.size(); ++i) {
            Code code = codes.get(i);
            Op op = code.getInstr();
            if (op == Op.FUNC) {
                begin.add(i);
            } else if (divider.contains(op)) {
                if (i + 1 < codes.size()) begin.add(i + 1);
                if (op != Op.RETURN) labels.put(op == Op.JUMP ? code.getOrd1() : code.getRes(), -1);
            }
        }
        for (int i = 0; i < codes.size(); ++i) {
            Code code = codes.get(i);
            Op op = code.getInstr();
            if (op == Op.LABEL && labels.containsKey(code.getOrd1())) begin.add(i);
        }
        ArrayList<Integer> beginList = new ArrayList<>(begin);
        beginList.sort(Integer::compareTo);

        // 标记block结束code
        ArrayList<Integer> endList = new ArrayList<>();
        for (int i = 0; i < beginList.size(); ++i) {
            if (i + 1 < beginList.size()) endList.add(beginList.get(i + 1) - 1);
            else endList.add(codes.size() - 1);
        }

        // 生成block
        ArrayList<Block> blocks = new ArrayList<>();
        for (int i = 0; i < beginList.size(); ++i) {
            blocks.add(new Block(i, new ArrayList<Code>(codes.subList(beginList.get(i), endList.get(i) + 1))));
            // label is the beginning of a block
            if (codes.get(beginList.get(i)).getInstr() == Op.LABEL) {
                labels.put(codes.get(beginList.get(i)).getOrd1(), i);
            }
        }


        // build the graph
        // set nxt
        for (int i = 0; i < blocks.size(); ++i) {
            Block block = blocks.get(i);
            Code code = block.getCodes().get(block.getCodes().size() - 1);
            Op op = code.getInstr();
            if (op == Op.RETURN) {
                block.getNxt().add(-1);
            } else if (op == Op.JUMP) {
                block.getNxt().add(labels.get(code.getOrd1()));
            } else if (op == Op.NEZ_JUMP || op == Op.EQZ_JUMP) {
                if (i + 1 < blocks.size()) block.getNxt().add(i + 1);
                block.getNxt().add(labels.get(code.getRes()));
            } else {
                if (i + 1 < blocks.size()) block.getNxt().add(i + 1);
            }
        }
        // set pre
        blocks.get(0).getPre().add(-1);
        for (int i = 0; i < blocks.size(); ++i) {
            Block block = blocks.get(i);
            for (int nxt : block.getNxt()) {
                if (nxt != -1) blocks.get(nxt).getPre().add(i);
            }
        }

        func2blocks.put(func, blocks);
    }

    public void printGraph() {
        StringBuilder sb = new StringBuilder();
        for (String func : func2blocks.keySet()) {
            System.out.println("\nfunc: " + func);
            for (Block block : func2blocks.get(func)) {
                System.out.println(block);
            }
        }
        System.out.println();
    }
}
