package Symbol;

import Syntax.Decl.InitVal.InitVal;
import Syntax.Util.Index;

import java.util.ArrayList;

public class Val implements Symbol {
    private final String name;
    private final boolean isConst;
    private final InitVal initVal;
    private int addr;
    private final int dim;
    private final ArrayList<Index> dims = new ArrayList<>();
    private final int loc;

    public Val(String name, boolean isConst, InitVal initVal, int dim, ArrayList<Index> dims, int loc) {
        this.name = name;
        this.isConst = isConst;
        this.initVal = initVal;
        this.dim = dim;
        if (dims != null ) this.dims.addAll(dims);
        this.loc = loc;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isConst() {
        return isConst;
    }

    @Override
    public int getLoc() {
        return loc;
    }

    @Override
    public Integer setAndGetAddr(int addr) {
        this.addr = addr;
        return addr + getSize();
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
    }
     */

    public Integer getAddr() {
        return addr;
    }

    @Override
    public String toString() {
        return "val: " + name + " isConst: " + isConst + " dim: " + dim;
    }
}
