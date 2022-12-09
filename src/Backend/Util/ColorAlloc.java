package Backend.Util;

import Middle.Optimization.Block;
import Middle.Util.Code;
import Middle.Visitor;
import Symbol.Func;
import Symbol.Symbol;

import java.util.*;
import java.util.stream.Collectors;

public class ColorAlloc {
    public final ArrayList<Set<Integer>> G = new ArrayList<>();
    public final HashMap<Symbol, Integer> mp = new HashMap<>();
    public final HashMap<Integer, Symbol> mpR = new HashMap<>();
    public final HashSet<Integer> used = new HashSet<>();
    public int[] regAlloc;
    public String name;
    public int tot = 0;
    private int cnt = 0;

    public ColorAlloc(String name, ArrayList<Block> bks) {
        // .out.println("ColorAlloc: " + name);
        G.add(new HashSet<>());
        this.name = name;
        Func f = (Func) Visitor.str2Symbol.get(name);
        ArrayList<Symbol> param = new ArrayList<Symbol>(f.getFuncTable().getOrderSymbols().subList(0, f.getNum())); // 形参
        for (Symbol s1 : param) {
            for (Symbol s2 : param) add(s1, s2);
        }

        for (Block b : bks) {
            HashMap<Integer, HashSet<Block.Meta>> aco = b.calcCodeActiveOut();
            for (int i = 0; i < b.getCodes().size(); ++i) {
                Code c = b.getCodes().get(i);
                for (Symbol s : aco.get(i).stream().map(Block.Meta::getSymbol).collect(Collectors.toCollection(HashSet::new))) {
                    add(s, c.getConflict());
                }
                for (Symbol s1 : c.getUse().keySet()) {
                    for (Symbol s2 : c.getUse().keySet()) add(s1, s2);
                }
                for (Symbol s1 : c.getUse().keySet()) {
                    for (Symbol s : aco.get(i).stream().map(Block.Meta::getSymbol).collect(Collectors.toCollection(HashSet::new))) {
                        add(s, s1);
                    }
                }
                if ((c.getInstr() == Code.Op.DIV ||
                        c.getInstr() == Code.Op.MOD || c.getInstr() == Code.Op.MUL) && "-0123456789".indexOf(c.getOrd2().charAt(0)) == -1)
                    add(c.getSymbolRes(), c.getSymbolOrd1());
            }
        }
    }

    public boolean chk(Symbol u) {
        return u != null && u.getDim() == 0 && u.getBlockLevel() != 0;
    }

    public void add(Symbol u) {
        if (chk(u) && !mp.containsKey(u)) {
            mp.put(u, ++cnt);
            mpR.put(cnt, u);
            G.add(new HashSet<>());
        }
    }

    public void add(Symbol u, Symbol v) {
        add(u);
        add(v);
        if (chk(u) && chk(v) && u != v) {
            int iu = mp.get(u), iv = mp.get(v);
            G.get(iu).add(iv);
            G.get(iv).add(iu);
        }
    }

    private int[] deduce() {
        int[] p = new int[cnt + 1], h = new int[cnt + 1];
        int[] nxt = new int[cnt + 1], lst = new int[cnt + 1];
        int[] deg = new int[cnt + 1];
        boolean[] tf = new boolean[cnt + 1];

        Arrays.fill(tf, false);
        Arrays.fill(deg, 0);
        Arrays.fill(h, 0);
        h[0] = 1;
        for (int i = 0; i <= cnt; ++i) {
            nxt[i] = i + 1;
            lst[i] = i - 1;
        }
        nxt[cnt] = 0;

        int cur = cnt, nww = 0;
        while (cur != 0) {
            p[cur] = h[nww];
            h[nww] = nxt[h[nww]];
            lst[h[nww]] = 0;
            lst[p[cur]] = nxt[p[cur]] = 0;
            tf[p[cur]] = true;
            for (int v : G.get(p[cur]))
                if (!tf[v]) {
                    if (h[deg[v]] == v) h[deg[v]] = nxt[v];
                    nxt[lst[v]] = nxt[v];
                    lst[nxt[v]] = lst[v];
                    lst[v] = nxt[v] = 0;
                    ++deg[v];
                    nxt[v] = h[deg[v]];
                    lst[h[deg[v]]] = v;
                    h[deg[v]] = v;
                }
            --cur;
            if (h[nww + 1] != 0) ++nww;
            while (nww > 0 && h[nww] == 0) --nww;
        }

        return p;
    }

    private int[] color(int[] p) {
        int[] tag = new int[p.length + 1];
        int[] res = new int[p.length];
        Arrays.fill(res, 0);
        Arrays.fill(tag, 0);
        int cnt = 0;

        for (int i = p.length - 1, x; i > 0; --i) {
            x = p[i];
            for (int v : G.get(x)) tag[res[v]] = x;
            int c = 1;
            while (tag[c] == x) ++c;
            res[x] = c;
            cnt = Math.max(cnt, c);
        }
        res[0] = cnt;
        return res;
    }

    public void alloc() {
        // color: 1, 2, 3 ...
        int[] colors = color(deduce()); // 0: tot amount of color, 1: color1, 2: color2, ...
        int[] colorSum = new int[colors[0] + 1]; // 0 is the number of colors

        for (int c : colors) ++colorSum[c]; // 统计每种颜色对应变量的数量
        --colorSum[colors[0]];

        List<Integer> order = new ArrayList<>();
        for (int i = 1; i < colorSum.length; ++i) order.add(i);
        order.sort((x, y) -> Integer.compare(colorSum[y], colorSum[x])); // order[i] = j 表示颜色j对应变量数量排名第i
        for (int i = 0; i < order.size(); ++i) colorSum[order.get(i)] = i; // colorSum[i] = j 表示颜色i对应变量数量排名第j
        // 排名从0开始，排名数值越小，对应变量数量越多
        regAlloc = new int[colors.length];
        for (int i = 1; i < colors.length; ++i) {
            regAlloc[i] = colorSum[colors[i]]; // regAlloc[i] = j 表示变量i对应的寄存器编号为j
            if (regAlloc[i] >= RegAlloc.Regs.length - 2) regAlloc[i] = -1; // 留出两个寄存器
        }
        // print() regAlloc
        System.out.println("regAlloc: " + name);
        for (int i = 1; i < regAlloc.length; ++i) {
            if (regAlloc[i] != -1) {
                System.out.println(mpR.get(i).getName() + ": " + RegAlloc.Regs[regAlloc[i]]);
            }
        }
        tot = Math.min(colors[0], RegAlloc.Regs.length - 2);
    }

    // public HashMap<Symbol, Integer> param = new HashMap<Symbol, Integer>();
}
