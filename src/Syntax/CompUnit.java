package Syntax;

import Syntax.Decl.Decl;
import Syntax.Func.FuncDef;
import Syntax.Func.MainFuncDef;

import java.util.ArrayList;

public class CompUnit {
    private final ArrayList<Decl> decls = new ArrayList<>();
    private final ArrayList<FuncDef> funcDefs = new ArrayList<>();
    private MainFuncDef mainFuncDef;

    public void addDecl(Decl decl) {
        decls.add(decl);
    }

    public void addFuncDef(FuncDef funcDef) {
        funcDefs.add(funcDef);
    }

    public void setMainFuncDef(MainFuncDef mainFuncDef) {
        this.mainFuncDef = mainFuncDef;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Decl decl : decls) sb.append(decl).append("\n");
        for (FuncDef funcDef : funcDefs) sb.append(funcDef).append("\n");
        sb.append(mainFuncDef).append("\n");
        return sb + "<CompUnit>";
    }

    public ArrayList<Decl> getDecls() {
        return decls;
    }

    public ArrayList<FuncDef> getFuncDefs() {
        return funcDefs;
    }

    public MainFuncDef getMainFuncDef() {
        return mainFuncDef;
    }
}
