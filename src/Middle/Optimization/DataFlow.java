package Middle.Optimization;

import Middle.Util.Code;
import Middle.Util.Code.Op;
import Symbol.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DataFlow {
    private static HashMap<String, ArrayList<Block>> func2blocks = new HashMap<>();
    private final HashMap<String, ArrayList<Code>> func2codes = new HashMap<>();
    public static HashSet<Op> divider = new HashSet<Op>() {{
        // add(Op.LABEL);
        add(Op.JUMP);
        add(Op.NEZ_JUMP);
        add(Op.EQZ_JUMP);
        add(Op.RETURN); // exit --> size - 1
        // add(Op.FUNC); // entry --> 0
    }};

    public DataFlow(ArrayList<Code> codes) {
        func2blocks = new HashMap<>();
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
        for (String func : func2codes.keySet()) arriveDataAnalysis(func);
        for (String func : func2codes.keySet()) activeDataAnalysis(func);
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
        // set entry block
        blocks.add(new Block(0, new ArrayList<>(), func));
        for (int i = 0; i < beginList.size(); ++i) {
            blocks.add(new Block(i + 1, new ArrayList<Code>(codes.subList(beginList.get(i), endList.get(i) + 1)), func));
            // label is the beginning of a block
            if (codes.get(beginList.get(i)).getInstr() == Op.LABEL) {
                labels.put(codes.get(beginList.get(i)).getOrd1(), i + 1);
            }
        }
        // set exit block
        blocks.add(new Block(blocks.size(), new ArrayList<>(), func));

        // build the graph
        // set nxt
        int exit = blocks.size() - 1, entry = 0;
        blocks.get(entry).getNxt().add(1);
        for (int i = 1; i < blocks.size() - 1; ++i) {
            Block block = blocks.get(i);
            Code code = block.getCodes().get(block.getCodes().size() - 1);
            Op op = code.getInstr();
            if (op == Op.RETURN) {
                block.getNxt().add(exit);
            } else if (op == Op.JUMP) {
                block.getNxt().add(labels.get(code.getOrd1()));
            } else if (op == Op.NEZ_JUMP || op == Op.EQZ_JUMP) {
                if (i + 1 < blocks.size() - 1) block.getNxt().add(i + 1);
                block.getNxt().add(labels.get(code.getRes()));
            } else {
                if (i + 1 < blocks.size() - 1) block.getNxt().add(i + 1);
            }
        }
        // set pre
        for (int i = 0; i < blocks.size(); ++i) {
            Block block = blocks.get(i);
            for (int nxt : block.getNxt()) blocks.get(nxt).getPre().add(i);
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


    // ------------------ arrive data analysis ------------------

    public static HashSet<Block.Meta> prepareKill(String fun, Integer blockId) {
        ArrayList<Block> blocks = func2blocks.get(fun);
        HashSet<Block.Meta> kill = new HashSet<>();
        HashSet<Symbol> genRes = new HashSet<>();
        blocks.get(blockId).getArriveGen().forEach(meta -> genRes.add(meta.getSymbol()));
        for (int i = 0; i < blocks.size(); ++i) {
            if (i == blockId) continue;
            ArrayList<Code> codes = blocks.get(i).getCodes();
            for (int j = 0; j < codes.size(); ++j) {
                Code code = codes.get(j);
                if (code.getGen() != null && genRes.contains(code.getSymbolRes())) kill.add(new Block.Meta(i, j, code, code.getSymbolRes()));
            }
        }
        return kill;
    }

    private void arriveDataAnalysis(String fun) {
        ArrayList<Block> blocks = func2blocks.get(fun);
        // init
        for (Block block : blocks) block.calcArriveGenAndKill();
        // iterate
        boolean changed;
        do {
            changed = false;
            for (int i = 1; i < blocks.size(); ++i) {
                Block block = blocks.get(i);
                HashSet<Block.Meta> in = new HashSet<>();
                for (int pre : block.getPre()) in.addAll(blocks.get(pre).getArriveOut());
                HashSet<Block.Meta> out = new HashSet<>(in);
                out.removeAll(block.getArriveKill());
                out.addAll(block.getArriveGen());
                if (!block.getArriveIn().equals(in) || !block.getArriveOut().equals(out)) {
                    block.setArriveIn(in);
                    block.setArriveOut(out);
                    changed = true;
                }
            }
        } while (changed);
    }

    // ------------------ active data analysis ------------------

    private void activeDataAnalysis(String fun) {
        ArrayList<Block> blocks = func2blocks.get(fun);
        // init
        for (Block block : blocks) block.calcActiveUseAndDef();
        // iterate
        boolean changed;
        do {
            changed = false;
            for (int i = blocks.size() - 1; i >= 1; --i) {
                Block block = blocks.get(i);
                HashSet<Block.Meta> out = new HashSet<>();
                for (int nxt : block.getNxt()) out.addAll(blocks.get(nxt).getActiveIn());
                HashSet<Block.Meta> in = new HashSet<>(out);
                in.removeAll(block.getActiveDef());
                in.addAll(block.getActiveUse());
                if (!block.getActiveIn().equals(in) || !block.getActiveOut().equals(out)) {
                    block.setActiveIn(in);
                    block.setActiveOut(out);
                    changed = true;
                }
            }
        } while (changed);
    }
}
