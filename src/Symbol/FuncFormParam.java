package Symbol;

import Syntax.Util.Index;

import java.util.ArrayList;

public class FuncFormParam implements Symbol {
    private final String name;
    private int addr;
    private final int dim;
    private final ArrayList<Index> dims = new ArrayList<>();
    private final int loc;

    public FuncFormParam(String name, int dim, ArrayList<Index> dims, int loc) {
        this.name = name;
        this.dim = dim;
        if (dims != null )this.dims.addAll(dims);
        this.loc = loc;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLoc() {
        return loc;
    }

    @Override
    public Integer setAndGetAddr(int addr) {
        this.addr = addr;
        return addr + 4;
    }

    @Override
    public Integer getAddr() {
        return addr;
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public Integer getSize() {
        /*int sz = 1;
        for (Integer x : dims) sz *= x;
        return sz * 4;*/
        return 0;
    }

    @Override
    public int getDim() {
        return dim;
    }

    /*
    public ArrayList<Integer> getDims() {
        return dims;
    }*/

    @Override
    public String toString() {
        return "funcParam: " + name + " dim: " + dim;
    }
}
