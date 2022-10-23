package Middle;

import Error.Error;
import Lexer.Token;
import Lexer.Type;
import Symbol.*;
import Syntax.CompUnit;
import Syntax.Decl.Decl;
import Syntax.Decl.Def;
import Error.*;
import Syntax.Decl.InitVal.InitArr;
import Syntax.Decl.InitVal.InitExp;
import Syntax.Decl.InitVal.InitVal;
import Syntax.Expr.Multi.*;
import Syntax.Expr.Unary.FuncRParams;
import Syntax.Expr.Unary.LVal;
import Syntax.Expr.Unary.PrimaryExp;
import Syntax.Expr.Unary.UnaryExp;
import Syntax.Func.Block;
import Syntax.Func.FuncDef;
import Syntax.Func.FuncFParam;
import Syntax.Func.FuncFParams;
import Syntax.Stmt.IfStmt;
import Syntax.Stmt.Simple.*;
import Syntax.Stmt.Stmt;
import Syntax.Stmt.WhileStmt;
import Syntax.Util.Index;

import java.util.ArrayList;
import java.util.List;

public class Visitor {
    private int blockLevel = 0;
    private int cycLevel = 0;
    public static SymbolTable curTable;
    private Func curFunc = null;
    private int magic = 1; // 用以生成错误的函数的新名字（(error)magic）加入符号表，否则无法检查重名函数的内部

    public Visitor(CompUnit compUnit) {
        compUnitTravel(compUnit);
    }

    private void compUnitTravel(CompUnit compUnit) {
        curTable = new SymbolTable(null);
        for (Decl decl : compUnit.getDecls()) declTravel(decl);
        for (FuncDef funcDef : compUnit.getFuncDefs()) funcTravel(funcDef);
        funcTravel(compUnit.getMainFuncDef());
    }

    private void back() {
        curTable = curTable.getFa();
        --blockLevel;
    }

    // -------------------- Decl -----------------------------------

    private void declTravel(Decl decl) {
        defTravel(decl.getFirst());
        for (Def def : decl.getDefs()) defTravel(def);
    }

    /*
    -------------------------------
    Error : b
     */
    private void defTravel(Def def) {
        Token ident = def.getIdentTk();
        String name = ident.getStrVal();
        boolean isConst = def.isConst();
        // b
        // tips: 位于函数顶层且和形参相同也算重复定义，和函数名重复不算重定义
        if (curTable.contains(name, false)) {
            ErrorTable.add(new Error(Error.Type.DUPLICATE_IDENT, ident.getLine()));
            return;
        }
        if (!def.isArr()) {
            if (!def.isInit()) {
                // 非数组且无初始化
                curTable.add(new Val(name, isConst, null, 0, null, blockLevel));
            } else {
                // 非数组且初始化
                curTable.add(new Val(name, isConst, def.getInitVal(), 0, null, blockLevel));
                initValTravel(def.getInitVal());
            }
        } else {

            // ConstExp
            for (Index index : def.getIndexes()) {
                Exp exp = index.getExp();
                constExpTravel((ConstExp) exp);
            }

            if (!def.isInit()) {
                // 数组且非初始化
                curTable.add(new Val(name, isConst, null, def.getDim(), def.getIndexes(), blockLevel));
            } else {
                // 数组且初始化
                curTable.add(new Val(name, isConst, def.getInitVal(), def.getDim(), def.getIndexes(), blockLevel));
                initValTravel(def.getInitVal());
            }
        }
    }

    private void initValTravel(InitVal initVal) {
        if (initVal instanceof InitArr) {

           InitVal v = ((InitArr) initVal).getFirst();
           initValTravel(v);
           for (InitVal var : ((InitArr) initVal).getVars()) {
               initValTravel(var);
           }

        } else if (initVal instanceof InitExp) {

            if (initVal.isConst()) {
                constExpTravel((ConstExp) ((InitExp) initVal).getExp());
            } else {
                expTravel(((InitExp) initVal).getExp());
            }

        }
    }


    // ------------------- FUNC ---------------------------------

    /*
    -------------------------------
    Error : b, g
     */
    private void funcTravel(FuncDef funcDef) {
        Token ident = funcDef.getIdentTk();
        String name = ident.getStrVal();

        // b
        // tips : 如果函数名重复需要给出函数体内的错误
        // 故不能直接return
        if (curTable.contains(name, false)) {
            ErrorTable.add(new Error(Error.Type.DUPLICATE_IDENT, ident.getLine()));
            name += "(error" + ++magic + ")";
        }

        // 处理符号表
        curTable = new SymbolTable(curTable);
        Func func = new Func(name, funcDef.getFuncType().getType() == Type.VOIDTK ? Func.Type.voidFunc : Func.Type.intFunc, funcDef.getParamNum(), ++blockLevel, curTable);
        curTable.getFa().add(func);
        curFunc = func;
        ++blockLevel;

        // 检查形参
        if (funcDef.hasParams()) {
            FuncFParams funcFParams = funcDef.getFuncFParams();
            FuncFParam first = funcFParams.getFirst();
            funcFParamTravel(first);
            for (FuncFParam funcFParam : funcFParams.getFuncFParams()) funcFParamTravel(funcFParam);
        }

        // 处理函数体
        blockTravel(funcDef.getBlock(), true);

        // g
        Block.BlockItem lastBlockItem = funcDef.getLastBlockItem();
        SimpleStmt spl = null;
        if (lastBlockItem != null && lastBlockItem.getStmt() != null && lastBlockItem.getStmt().getSpl() != null)
            spl = lastBlockItem.getStmt().getSpl();
        if (curFunc.getType() == Func.Type.intFunc && (spl == null || !(spl.getSpl() instanceof ReturnStmt)))
            ErrorTable.add(new Error(Error.Type.LACK_RETURN_VALUE, funcDef.getBlock().getrBTK().getLine()));

        // 回撤
        back();
    }

    /*
   -------------------------------
   Error : b
    */
    private void funcFParamTravel(FuncFParam funcFParam) {
        Token ident = funcFParam.getIdent();
        String name = ident.getStrVal();
        // b
        // 形参间不能重名
        if (curTable.contains(name, false)) {
            ErrorTable.add(new Error(Error.Type.DUPLICATE_IDENT, ident.getLine()));
            return;
        }

        FuncFormParam param;
        if (funcFParam.isArr()) {

            // 第一维都不存在
            for (Index index : funcFParam.getIndexes()) {
                Exp exp = index.getExp();
                if (exp != null) constExpTravel((ConstExp) exp);
            }

            param = new FuncFormParam(name, funcFParam.getDim(), funcFParam.getIndexes(), blockLevel);
        } else {
            param = new FuncFormParam(name, 0, null, blockLevel);
        }
        curTable.add(param);
    }

    private void blockTravel(Block block, boolean isFunc) {
       ArrayList<Block.BlockItem> blockItems = block.getBlockItems();

       if (!isFunc) {
           ++blockLevel;
           curTable = new SymbolTable(curTable);
       }

       for (Block.BlockItem blockItem : blockItems) {
           if (blockItem.getDecl() != null) declTravel(blockItem.getDecl());
           else stmtTravel(blockItem.getStmt());
       }

       if (!isFunc) back();
    }

    // ------------------------------ STMT -------------------------

    private void stmtTravel(Stmt stmt) {
        if (stmt.getBlockStmt() != null) {
            blockTravel(stmt.getBlockStmt().getBlock(), false);
        } else if (stmt.getIfStmt() != null) {
            ifStmtTravel(stmt.getIfStmt());
        } else if (stmt.getWhileStmt() != null) {
            whileStmtTravel(stmt.getWhileStmt());
        } else {
            Simple spl = stmt.getSpl().getSpl();

            if (spl == null) return;

            if (spl instanceof InputStmt) inputStmtTravel((InputStmt) spl);
            else if (spl instanceof AssignStmt) assignStmtTravel((AssignStmt) spl);
            else if (spl instanceof ExpStmt) expStmtTravel((ExpStmt) spl);
            else if (spl instanceof LoopStmt) loopStmtTravel((LoopStmt) spl);
            else if (spl instanceof OutputStmt) outputStmtTravel((OutputStmt) spl);
            else if (spl instanceof ReturnStmt) returnStmtTravel((ReturnStmt) spl);
        }
    }

    private void ifStmtTravel(IfStmt ifStmt) {
        condTravel(ifStmt.getCond());
        stmtTravel(ifStmt.getIfStmt());
        if (ifStmt.hasElse()) {
            stmtTravel(ifStmt.getElseStmt());
        }
    }

    private void whileStmtTravel(WhileStmt whileStmt) {
        ++cycLevel;

        condTravel(whileStmt.getCond());
        stmtTravel(whileStmt.getStmt());

        --cycLevel;
    }

    /*
    -------------------------------------
    Error: h
     */
    private void inputStmtTravel(InputStmt inputStmt) {
        // h
        Token ident = inputStmt.getLVal().getIdentTk();
        String name = ident.getStrVal();
        if (curTable.contains(name, true) && curTable.get(name, true).isConst()) {
            ErrorTable.add(new Error(Error.Type.CHANGE_CONST_VALUE, ident.getLine()));
        }

        lValTravel(inputStmt.getLVal());
    }

    /*
    ---------------------------
    Error: h
     */
    private void assignStmtTravel(AssignStmt assignStmt) {
        // h
        Token ident = assignStmt.getlVal().getIdentTk();
        String name = ident.getStrVal();
        if (curTable.contains(name, true) && curTable.get(name, true).isConst()) {
            ErrorTable.add(new Error(Error.Type.CHANGE_CONST_VALUE, ident.getLine()));
        }

        lValTravel(assignStmt.getlVal());
        expTravel(assignStmt.getExp());
    }

    private void expStmtTravel(ExpStmt expStmt) {
        expTravel(expStmt.getExp());
    }

    /*
    ----------------------------------
    Error: m
     */
    private void loopStmtTravel(LoopStmt loopStmt) {
        // m
        if (cycLevel == 0) ErrorTable.add(new Error(Error.Type.BREAK_CONTINUE_OUT_LOOP, loopStmt.getContrTK().getLine()));
    }

    /*
    ----------------------------------------
    Error: l
     */
    private void outputStmtTravel(OutputStmt outputStmt) {
        ArrayList<Exp> exps = outputStmt.getExps();
        for (Exp exp : exps) expTravel(exp);

        String fs = outputStmt.getsTK().getStrVal();
        int cnt = 0;
        for (int i = 0; i < fs.length(); ++i) {
            if (fs.charAt(i) == '%' && i + 1 < fs.length() && fs.charAt(i + 1) == 'd') ++cnt;
        }

        // l
        if (cnt != exps.size()) ErrorTable.add(new Error(Error.Type.MISMATCH_PRINT_FORMAT_CHAR, outputStmt.getPrintTK().getLine()));
    }

    /*
    ---------------------------------------
    Error: f, g
     */
    private void returnStmtTravel(ReturnStmt returnStmt) {
        // f
        if (returnStmt.getExp() != null && curFunc.getType() == Func.Type.voidFunc) {
            ErrorTable.add(new Error(Error.Type.SPARE_RETURN_VALUE, returnStmt.getRetTK().getLine()));
            return;
        }
       if (returnStmt.getExp() != null) expTravel(returnStmt.getExp());
    }

    // -------------------------- EXP ----------------------------

    private void lOrExpTravel(LOrExp lOrExp) {
        LAndExp first = lOrExp.getFirst();
        lAndExpTravel(first);
        for (LAndExp lAndExp : lOrExp.getTs()) {
            lAndExpTravel(lAndExp);
        }
    }

    private void funcRParamsTravel(FuncRParams funcRParams, Token ident , String name) {
        assert curTable.get(name, true) != null && curTable.get(name, true) instanceof Func;
        Func func = (Func) curTable.get(name, true);

        if (funcRParams != null) {
            // d
            if (func.getNum() != funcRParams.getNum()) {
                ErrorTable.add(new Error(Error.Type.MISMATCH_PARAM_NUM, ident.getLine()));
                return;
            }

            Exp first = funcRParams.getFirst();
            expTravel(first);

            // e
            List<Symbol> symbols = func.getSymbolTable().getOtherSymbols().subList(0, func.getNum());
            int dim = first.getFormDim();
            if (dim != -10000 && dim != symbols.get(0).getDim()) {
                ErrorTable.add(new Error(Error.Type.MISMATCH_PARAM_TYPE, ident.getLine()));
                return;
            } else if (dim == -10000) {
                return;
            }

            for (int i = 0; i < funcRParams.getExps().size(); ++i) {
                Exp exp = funcRParams.getExps().get(i);
                expTravel(exp);

                // e
                dim = exp.getFormDim();
                if (dim != -10000 && dim != symbols.get(i + 1).getDim()) {
                    ErrorTable.add(new Error(Error.Type.MISMATCH_PARAM_TYPE, ident.getLine()));
                    return;
                } else if (dim == -10000) {
                    return;
                }
            }

        } else {

            // d
            if (func.getNum() != 0) {
                ErrorTable.add(new Error(Error.Type.MISMATCH_PARAM_NUM, ident.getLine()));
            }
        }
    }

    private void primaryExpTravel(PrimaryExp primaryExp) {
        LVal lVal = primaryExp.getLval();
        Exp exp = primaryExp.getExp();

        if (exp != null) {
            expTravel(exp);
        } else if (lVal != null) {
            lValTravel(lVal);
        }
        // number
    }

    /*
    函数调用只占一行
    -------------------------------------
    Error: c, d, e
     */
    private void unaryExpTravel(UnaryExp unaryExp) {
        PrimaryExp primaryExp = unaryExp.getPrimaryExp();
        UnaryExp innerUnaryExp = unaryExp.getUnaryExp();
        Token ident = unaryExp.getIdentTk();

        if (primaryExp != null) {
            primaryExpTravel(primaryExp);
        } else if (innerUnaryExp != null) {
            unaryExpTravel(innerUnaryExp);
        } else {

            // 检查函数调用
            assert ident != null;
            String name = ident.getStrVal();

            // c
            if (!curTable.contains(name, true)) {
                ErrorTable.add(new Error(Error.Type.UNDEFINE_IDENT, ident.getLine()));
                return;
            }

            FuncRParams funcRParams = unaryExp.getFuncRParams();
            funcRParamsTravel(funcRParams, ident, name);

        }
    }

    private void mulExpTravel(MulExp mulExp) {
        UnaryExp first = mulExp.getFirst();
        unaryExpTravel(first);
        for (UnaryExp unaryExp : mulExp.getTs()) {
            unaryExpTravel(unaryExp);
        }
    }

    private void addExpTravel(AddExp addExp) {
        MulExp first = addExp.getFirst();
        mulExpTravel(first);
        for (MulExp mulExp : addExp.getTs()) {
            mulExpTravel(mulExp);
        }
    }

    private void relExpTravel(RelExp relExp) {
        AddExp first = relExp.getFirst();
        addExpTravel(first);
        for (AddExp addExp : relExp.getTs()) {
            addExpTravel(addExp);
        }
    }

    private void eqExpTravel(EqExp eqExp) {
        RelExp first = eqExp.getFirst();
        relExpTravel(first);
        for (RelExp relExp : eqExp.getTs()) {
            relExpTravel(relExp);
        }
    }

    private void lAndExpTravel(LAndExp lAndExp) {
        EqExp first = lAndExp.getFirst();
        eqExpTravel(first);
        for (EqExp eqExp : lAndExp.getTs()) {
            eqExpTravel(eqExp);
        }
    }

    private void condTravel(Cond cond) {
        lOrExpTravel(cond.getlOrExp());
    }

    private void expTravel(Exp exp) {
        addExpTravel(exp.getAddExp());
    }

    private void constExpTravel(ConstExp constExp) {
        addExpTravel(constExp.getAddExp());
    }

    /*
    ------------------------------------
    Error: c
     */
    private void lValTravel(LVal lVal) {
        Token ident = lVal.getIdentTk();
        String name = ident.getStrVal();

        // c
        if (!curTable.contains(name, true)) {
            ErrorTable.add(new Error(Error.Type.UNDEFINE_IDENT, ident.getLine()));
        }

        for (Index index : lVal.getIndexes()) {
            Exp exp = index.getExp();
            expTravel(exp);
        }
    }
}
