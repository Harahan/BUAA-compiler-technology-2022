package Middle.Optimization;

import Middle.Util.Code;
import Middle.Visitor;
import Symbol.Symbol;

import java.util.*;
import java.util.stream.Collectors;

public class OptimizeLoop {
    public String name;
    public ArrayList<Block> nodes;
    public HashMap<Integer, HashSet<Integer>> dom = new HashMap<>();
    // 回边
    public HashSet<Pair> backEdge = new HashSet<>();

    public boolean ok = true;

    public HashMap<Integer, HashMap<Integer, HashSet<Block.Meta>>> codeDef = new HashMap<>();

    // 属于循环的节点
    public HashMap<Pair, HashSet<Integer>> loopNodeMap = new HashMap<>();
    public HashSet<Pair> deleteCodeMap = new HashSet<>();
    public HashMap<Integer, ArrayList<Code>> posCodeMap = new HashMap<>();


    public OptimizeLoop(String name, ArrayList<Block> nodes) {
        this.name = name;
        this.nodes = nodes;
        calcDom();
        calcBackEdge();
        calcLoop();
        calcCodeDef();
        extract();
        // 删除冗余代码
        for (Block b : nodes) {
            ArrayList<Code> newCodes = new ArrayList<>();
            for (int i = 0; i < b.getCodes().size(); ++i) {
                if (!deleteCodeMap.contains(new Pair(b.getId(), i))) {
                    newCodes.add(b.getCodes().get(i));
                }
            }
            b.setCodes(newCodes);
        }
        // 插入代码
        for (Integer i : posCodeMap.keySet()) {
            Block b = nodes.get(i);
            ArrayList<Code> codes = b.getCodes();
            codes.addAll(posCodeMap.get(i));
            b.setCodes(codes);
        }
    }

    // 计算必经节点集
    public void calcDom() {
        // 初始化
        for (int i = 0; i < nodes.size(); ++i) {
            dom.put(i, new HashSet<Integer>() {{ for (int j = 0; j < nodes.size(); ++j) add(j); }});
        }
        dom.put(0, new HashSet<Integer>() {{ add(0); }});
        // 计算
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 1; i < nodes.size(); ++i) {
                HashSet<Integer> newDom = new HashSet<>();
                boolean first = true;
                for (int j : nodes.get(i).getPre()) {
                    HashSet<Integer> tmp = new HashSet<>(dom.get(j));
                    if (!first) tmp.retainAll(newDom);
                    newDom = tmp;
                    first = false;
                }
                newDom.add(i);
                if (!newDom.equals(dom.get(i))) {
                    dom.put(i, newDom);
                    changed = true;
                }
            }
        }
    }

    // 计算回边
    public void calcBackEdge() {
        for (int i = 0; i < nodes.size(); ++i) {
            for (int j : nodes.get(i).getNxt()) {
                if (dom.get(i).contains(j)) {
                    backEdge.add(new Pair(i, j));
                }
            }
        }
    }

    // 计算循环
    public void calcLoop() {
        for (int i = 0; i < nodes.size(); ++i) {
            for (int j : nodes.get(i).getNxt()) {
                if (backEdge.contains(new Pair(i, j))) {
                    HashSet<Integer> loopNode = new HashSet<>();
                    loopNode.add(i);
                    ++nodes.get(i).lp;
                    Stack<Integer> stack = new Stack<>();
                    stack.push(i);
                    while (!stack.isEmpty()) {
                        int m = stack.pop();
                        for (int p : nodes.get(m).getPre()) {
                            if (!loopNode.contains(p) && p >= j) {
                                loopNode.add(p);
                                ++nodes.get(p).lp;
                                stack.push(p);
                            }
                        }
                    }
                   // i -> j 为回边
                    // j 为循环头
                    // j 一样就合并
                    boolean flag = false;
                    for (Pair p : loopNodeMap.keySet()) {
                        if (p.y == j) {
                            loopNodeMap.get(p).addAll(loopNode);
                            p.x = Math.min(p.x, nodes.get(j).lp);
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        loopNodeMap.put(new Pair(nodes.get(j).lp, j), loopNode);
                    }
                }
            }
        }
    }

    // 循环节点集为 loopNode 的循环，计算出口节点集
    public HashSet<Integer> calcExitNode(HashSet<Integer> loopNode) {
        HashSet<Integer> exitNode = new HashSet<>();
        for (int i : loopNode) {
            for (int j : nodes.get(i).getNxt()) {
                if (!loopNode.contains(j)) exitNode.add(i);
            }
        }
        return exitNode;
    }

    public boolean checkOrd(String s) {
        if ("-0123456789".indexOf(s.charAt(0)) != -1) return true;
        if (s.equals("(EMPTY)")) return true;
        if (s.equals("(RT)")) return false;
        Symbol sym = Visitor.str2Symbol.get(s);
        if (sym == null) return false;
        if (sym.getDim() != 0) return false;
        return sym.getBlockLevel() != 0;
    }

    // 计算所有代码的到达定值点
    public void calcCodeDef() {
        for (int i = 0; i < nodes.size(); ++i) {
            HashMap<Integer, HashSet<Block.Meta>> tmp = nodes.get(i).calcCodeArriveIn();
            codeDef.put(i, tmp);
        }
    }

    public HashSet<Block.Meta> getDef(int i, int j, Symbol sym) {
        return codeDef.get(i).get(j).stream().filter(x -> x.getSymbol().equals(sym)).collect(Collectors.toCollection(HashSet::new));
    }



    // 检查中间代码是否满足循环不变式
    public ArrayList<Block.Meta> checkInvariant(HashSet<Integer> loopNode) {
        // 一次检查loop中各个基本快的中间代码
        ArrayList<Block.Meta> invariant = new ArrayList<>();
        for (int i :loopNode) {
            ArrayList<Code> codes = nodes.get(i).getCodes();
            for (int j = 0; j < codes.size(); ++j) {
                Code code = codes.get(j);
                Code.Op op = code.getInstr();
                String s1 = code.getOrd1(), s2 = code.getOrd2(), res = code.getRes();
                if (Code.alu.contains(op) || op == Code.Op.DEF_VAL) {
                    if (checkOrd(s1) && checkOrd(s2) && checkOrd(res)) {
                        // 检查 s1 s2是否是循环不变量
                        // 循环不变量的定义点在循环之外
                        if ((code.getSymbolOrd1() == null || getDef(i, j, code.getSymbolOrd1()).stream().noneMatch(x -> loopNode.contains(x.getBlockId())))
                                && (code.getSymbolOrd2() == null || getDef(i, j, code.getSymbolOrd2()).stream().noneMatch(x -> loopNode.contains(x.getBlockId())))) {
                            invariant.add(new Block.Meta(i, j, code, code.getSymbolRes()));
                        }
                    }
                }
            }
        }
        // 只有一个到达定值点并且该点的代码已经被标记
        int sz;
        do {
            sz = invariant.size();
            for (int i : loopNode) {
                ArrayList<Code> codes = nodes.get(i).getCodes();
                for (int j = 0; j < codes.size(); ++j) {
                    Code code = codes.get(j);
                    Code.Op op = code.getInstr();
                    String s1 = code.getOrd1(), s2 = code.getOrd2(), res = code.getRes();
                    if (Code.alu.contains(op) || op == Code.Op.DEF_VAL) {
                        if (checkOrd(s1) && checkOrd(s2) && checkOrd(res)) {
                            // 检查 s1 s2 是否都是循环不变量
                            // 循环不变量的定义点在循环之外
                            // 或者是循环不变量的定义点在循环之内，但是该点的代码已经被标记，且唯一
                            boolean ck1 = code.getSymbolOrd1() == null || getDef(i, j, code.getSymbolOrd1()).stream().noneMatch(x -> loopNode.contains(x.getBlockId()))
                                    || (getDef(i, j, code.getSymbolOrd1()).size() == 1
                                    && invariant.containsAll(getDef(i, j, code.getSymbolOrd1())));
                            boolean ck2 = code.getSymbolOrd2() == null || getDef(i, j, code.getSymbolOrd2()).stream().noneMatch(x -> loopNode.contains(x.getBlockId()))
                                    || (getDef(i, j, code.getSymbolOrd2()).size() == 1
                                    && invariant.containsAll(getDef(i, j, code.getSymbolOrd2())));
                            if (ck1 && ck2 && !invariant.contains(new Block.Meta(i, j, code, code.getSymbolRes()))) invariant.add(new Block.Meta(i, j, code, code.getSymbolRes()));
                        }
                    }
                }
            }
        } while (sz != invariant.size());
        // System.out.println("Invariant: " + invariant);
        return invariant;
    }

    // 计算外提循环不变式的代码
    public ArrayList<Code> getExtractCode(HashSet<Integer> loopNode) {
        ArrayList<Code> extractCode = new ArrayList<>();
        ArrayList<Block.Meta> invariant = checkInvariant(loopNode);
        for (Block.Meta meta : invariant) {
            Code code = meta.getCode();
            Integer i = meta.getBlockId(), j = meta.getCodeId();
            // i 是所有出口节点的必经节点 || i 在所有出口的后继节点(不在循环中)不再活跃
            HashSet<Integer> exit = calcExitNode(loopNode);
            boolean ck1 = exit.stream().allMatch(x -> dom.get(x).contains(i));
            ck1 |= exit.stream().allMatch(x -> nodes.get(x).getNxt().stream().noneMatch(y -> !loopNode.contains(y) && nodes.get(y).getActiveIn().contains(new Block.Meta(-1, -1, null, code.getSymbolRes()))));
            // code 的 res 在循环其它节点没有被定义
            for (int k : loopNode) {
                Block block = nodes.get(k);
                for (int l = 0; l < block.getCodes().size(); ++l) {
                    if (k == i) continue;
                    Code code1 = block.getCodes().get(l);
                    // System.out.println(code1 + " ------------ " + code);
                    if (code1.getDef() != null && code1.getDef().equals(code.getSymbolRes())) {
                        ck1 = false;
                        break;
                    }
                }
            }
            // 循环中所有对于res 的引用只有code所在基本块可以到达
            for (int k : loopNode) {
                Block block = nodes.get(k);
                for (int l = 0; l < block.getCodes().size(); ++l) {
                    if (k == i) continue;
                    for (Symbol sym : block.getCodes().get(l).getUse().keySet()) {
                        if (sym != null && sym.equals(code.getSymbolRes())) {
                            if (!getDef(k, l, sym).stream().allMatch(x -> i.equals(x.getBlockId()))) {
                                ck1 = false;
                                break;
                            }
                        }
                    }
                }
            }
            // 寻找分量在循环中的定值点均已经加入到循环不变式中
            HashSet<Block.Meta> ordDefs = getDef(i, j, code.getSymbolOrd1()).stream().filter(x -> loopNode.contains(x.getBlockId())).collect(Collectors.toCollection(HashSet::new));
            ordDefs.addAll(getDef(i, j, code.getSymbolOrd2()).stream().filter(x -> loopNode.contains(x.getBlockId())).collect(Collectors.toCollection(HashSet::new)));
            ordDefs.removeIf(x -> Objects.equals(x.getBlockId(), i) && Objects.equals(x.getCodeId(), j));
            if (ordDefs.size() != 0 && !extractCode.containsAll(ordDefs.stream().map(Block.Meta::getCode).collect(Collectors.toCollection(ArrayList::new)))) ck1 = false;
            if (ck1 && !deleteCodeMap.contains(new Pair(i, j))) {
                extractCode.add(code);
                ok = false;
                // 删除循环中的代码
                deleteCodeMap.add(new Pair(i, j));
            }
        }
        // System.out.println("ExtractCode: " + extractCode);
        return extractCode;
    }

    public void extract() {
        ArrayList<Pair> pairList = new ArrayList<>(loopNodeMap.keySet());
        pairList.sort(Comparator.comparingInt(Pair::getX));
        for (Pair p : pairList) {
            int pre = p.y - 1;
            if (nodes.get(p.y).getPre().stream().filter(x -> !loopNodeMap.get(p).contains(x)).count() != 1 ||
                !nodes.get(pre).getNxt().contains(p.y)) {
                System.err.println("Loop " + p.y + " has more than one pre node");
                System.exit(1);
            }
            ArrayList<Code> extractCode = getExtractCode(loopNodeMap.get(p));
            posCodeMap.put(pre, extractCode);
        }
    }


    // Pair类
    private static class Pair{
        public int x, y;

        public Pair(int x, int y) {
            this.x = x;
            this.y = y;
        }

        // equals方法
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return x == pair.x &&
                    y == pair.y;
        }

        // hashCode方法
        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        public static int getX(Pair t) {
            return t.x;
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }
}
