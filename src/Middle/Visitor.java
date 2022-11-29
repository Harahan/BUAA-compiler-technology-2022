package Middle;

import Backend.MipsGenerator;
import Error.Error;
import Error.ErrorTable;
import Lexer.Token;
import Lexer.Type;
import Middle.Util.Calc;
import Middle.Util.Code;
import Symbol.*;
import Syntax.CompUnit;
import Syntax.Decl.Decl;
import Syntax.Decl.Def;
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
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class Visitor {
    private int blockLevel = 0;
    private int cycLevel = 0;
    public static SymbolTable curTable;
    private Func curFunc = null;
    private int magic = 1; // 用以生成错误的函数的新名字加入符号表，否则无法检查重名函数的内部

    public static SymbolTable global;
    public static HashMap<String, Symbol> str2Symbol = new HashMap<>();
    private static final HashMap<Integer, Integer> blockLevelNum = new HashMap<>();
    private final Stack<String> whileBeginEndLabelStack = new Stack<>();

    public Visitor(CompUnit compUnit) {
        compUnitTravel(compUnit);
    }

    private void compUnitTravel(CompUnit compUnit) {
        curTable = new SymbolTable(null, blockLevel, 1, false);
        global = curTable;
        blockLevelNum.put(0, 1);
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
    注意全局变量默认初始化为0
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
        String nickname;
        Val val;
        String x;
        if (!def.isArr()) {
            if (!def.isInit()) {
                // 非数组且无初始化
                val = new Val(name, isConst, 0, null, curTable);
                curTable.add(val);
                nickname = val.getNickname();
                MidCodeList.add(Code.Op.DEF_VAL, "(EMPTY)", "(EMPTY)", nickname);
                val.addInitVal(blockLevel == 0 ? 0 : null);
            } else {
                // 非数组且初始化
                x = expTravel(((InitExp)def.getInitVal()).getExp());
                val = new Val(name, isConst, 0, null, curTable);
                curTable.add(val);
                nickname = val.getNickname();
                MidCodeList.add(Code.Op.DEF_VAL, x, "(EMPTY)", nickname);
                try {
                    val.addInitVal(Integer.valueOf(x));
                    // if (nickname.equals("fff(1,1)")) System.out.println(x);
                } catch (Exception ignore) {
                    val.addInitVal(null);
                }
            }
        } else {

            // ConstExp
            // 计算 dims
            ArrayList<Integer> dims = new ArrayList<>();
            for (Index index : def.getIndexes()) {
                Exp exp = index.getExp();
                try {
                    dims.add(Integer.valueOf(expTravel(exp)));
                } catch (Exception ignore) {}
            }
            // System.out.println(dims + " " + name);

            if (!def.isInit()) {
                // 数组且非初始化
                val = new Val(name, isConst, def.getDim(), dims, curTable);
                curTable.add(val);
                nickname = val.getNickname();
                MidCodeList.add(Code.Op.DEF_ARR, "(EMPTY)", "(EMPTY)", nickname);
                if (dims.size() == 2) {
                    for (int i = 0; i < dims.get(0); ++i) {
                        for (int j = 0; j < dims.get(1); ++j) val.addInitVal(blockLevel == 0 ? 0 : null);
                    }
                } else {
                    for (int i = 0; i < dims.get(0); ++i) val.addInitVal(blockLevel == 0 ? 0 : null);
                }
            } else {
                // 数组且初始化
                val = new Val(name, isConst, def.getDim(), dims, curTable);
                curTable.add(val);
                nickname = val.getNickname();
                MidCodeList.add(Code.Op.DEF_ARR, "(EMPTY)", "(EMPTY)", nickname);
                int dim = def.getDim();
                ArrayList<InitVal> vars = ((InitArr) def.getInitVal()).getVars();
                if (dim == 2) {
                    // 2维
                    assert vars.size() == dims.get(0);
                    for (int i = 0; i < dims.get(0); ++i) {
                        // System.out.println(nickname);
                        ArrayList<InitVal> o = ((InitArr) vars.get(i)).getVars();

                        if (o.isEmpty()) { // {{} ...}
                            for (int j = 0; j < dims.get(1); ++j) {
                                val.addInitVal(0);
                                MidCodeList.add(Code.Op.ASSIGN, "0", "(EMPTY)", nickname + "[" + (i * dims.get(1) + j) + "]");
                            }
                        } else { // {{2, 2} ...}
                            assert o.size() == dims.get(1);
                            // System.out.println(o.size() + " " + dims.get(1));
                            for (int j = 0; j < dims.get(1); ++j) {
                                x = expTravel(((InitExp) o.get(j)).getExp());
                                try {
                                    val.addInitVal(Integer.valueOf(x));
                                } catch (Exception ignore) {
                                    val.addInitVal(null);
                                }
                                MidCodeList.add(Code.Op.ASSIGN, x, "(EMPTY)", nickname + "[" + (i * dims.get(1) + j) + "]");
                            }
                        }
                    }
                } else {
                    // 1维
                    if (vars.isEmpty()) {
                        for (int i = 0; i < dims.get(0); ++i) {
                            val.addInitVal(0);
                            MidCodeList.add(Code.Op.ASSIGN, "0", "(EMPTY)", nickname + "[" + i + "]");
                        }
                    } else {
                        assert vars.size() == dims.size();
                        for (int i = 0; i < dims.get(0); ++i) {
                            x = expTravel(((InitExp) vars.get(i)).getExp());
                            try {
                                val.addInitVal(Integer.valueOf(x));
                            } catch (Exception ignore) {
                                val.addInitVal(null);
                            }
                            MidCodeList.add(Code.Op.ASSIGN, x, "(EMPTY)", nickname + "[" + i + "]");
                        }
                    }
                }
                MidCodeList.add(Code.Op.END_DEF_ARR, "(EMPTY)", "(EMPTY)", nickname);
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
            name += "(ERROR_FUNC_" + ++magic + ")";
        }

        // 处理符号表
        ++blockLevel;
        curTable = new SymbolTable(curTable, blockLevel, blockLevelNum.getOrDefault(blockLevel, 0) + 1, true); // 已经更新
        blockLevelNum.merge(blockLevel, 1, Integer::sum);
        Func func = new Func(name, funcDef.getFuncType().getType() == Type.VOIDTK ? Func.Type.voidFunc : Func.Type.intFunc, funcDef.getParamNum(), curTable, curTable.getFa());
        curTable.getFa().add(func);
        curTable.getFa().add(curTable);
        curFunc = func;
        // System.out.println(curFunc);
        MidCodeList.add(Code.Op.FUNC, curFunc.getNickname(), "(EMPTY)", "(EMPTY)");

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
        if (curFunc.getType() == Func.Type.voidFunc && (spl == null || !(spl.getSpl() instanceof ReturnStmt)))
            MidCodeList.add(Code.Op.RETURN, "(EMPTY)", "(EMPTY)", "(EMPTY)");
        MidCodeList.add(Code.Op.FUNC_END, curFunc.getNickname(), "(EMPTY)", "(EMPTY)");
        back();
        curFunc = null;
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
            ArrayList<Integer> arr = new ArrayList<Integer>() {{add(-1);}};
            for (Index index : funcFParam.getIndexes()) {
                Exp exp = index.getExp();
                if (exp != null) {
                    try {
                        arr.add(Integer.valueOf(expTravel(exp)));
                    } catch (Exception ignore) {}
                }
            }

            param = new FuncFormParam(name, funcFParam.getDim(), arr, curTable);
        } else {
            param = new FuncFormParam(name, 0, null, curTable);
        }
        curTable.add(param);
    }

    private void blockTravel(Block block, boolean isFunc) {
       ArrayList<Block.BlockItem> blockItems = block.getBlockItems();
       if (!isFunc) {
           ++blockLevel;
           curTable = new SymbolTable(curTable, blockLevel, blockLevelNum.getOrDefault(blockLevel, 0) + 1, false);
           blockLevelNum.merge(blockLevel, 1, Integer::sum);
           curTable.getFa().add(curTable);
           // MidCodeList.add(Code.Op.BLOCK_BEGIN, curTable.getNickName(), "(EMPTY)", "(EMPTY)");
       }

       for (Block.BlockItem blockItem : blockItems) {
           if (blockItem.getDecl() != null) declTravel(blockItem.getDecl());
           else stmtTravel(blockItem.getStmt());
       }

       if (!isFunc) {
           // MidCodeList.add(Code.Op.BLOCK_END, curTable.getNickName(), "(EMPTY)", "(EMPTY)");
           back();
       }
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
        // 无else:
        // 满足: 顺序执行,不满足: jump label_1
        // {...}
        // jump label_2
        // label_1
        // label_2

        // 有else:
        // 满足: 顺序执行,不满足: jump label_1
        // {...}
        // jump label_2
        // label_1
        // {...}
        // label_2
        Integer label1 = ++MidCodeList.labelCounter;
        int label2 = ++MidCodeList.labelCounter;
        condTravel(ifStmt.getCond(), label1, false);
        stmtTravel(ifStmt.getIfStmt());
        if (ifStmt.hasElse()) MidCodeList.add(Code.Op.JUMP, "(LABEL" + label2 + ")", "(EMPTY)", "(EMPTY)");
        MidCodeList.add(Code.Op.LABEL, "(LABEL" + label1 + ")", "(EMPTY)", "(EMPTY)");
        if (ifStmt.hasElse()) {
            stmtTravel(ifStmt.getElseStmt());
            // MidCodeList.add(Code.Op.JUMP, "(LABEL" + label2 + ")", "(EMPTY)", "(EMPTY)");
            // !!! 空jump
        }
        if (ifStmt.hasElse()) MidCodeList.add(Code.Op.LABEL, "(LABEL" + label2 + ")", "(EMPTY)", "(EMPTY)");
    }

    private void whileStmtTravel(WhileStmt whileStmt) {
        // label:
        // ...不满足: 跳label_end, 满足: 顺序执行
        // { ... }
        // jump label
        // label_end
        ++cycLevel;
        // !!! 空jump
        // String whileBeginLabel = MidCodeList.add(Code.Op.JUMP, "(AUTO)", "(EMPTY)", "(EMPTY)");
        String whileBeginLabel = "(LABEL" + ++MidCodeList.labelCounter + ")";
        MidCodeList.add(Code.Op.LABEL, whileBeginLabel, "(EMPTY)", "(EMPTY)");
        Integer realWhileBeginLabel = null;
        if (MipsGenerator.optimize.get("JumpOptimize")) realWhileBeginLabel = ++MidCodeList.labelCounter;
        // get end
        Integer label = ++MidCodeList.labelCounter;

        whileBeginEndLabelStack.push(whileBeginLabel);
        whileBeginEndLabelStack.push("(LABEL" + label + ")");

        condTravel(whileStmt.getCond(), label, false);
        // !!! 空jump
        if (MipsGenerator.optimize.get("JumpOptimize")) {
            // MidCodeList.add(Code.Op.JUMP, "(LABEL" + realWhileBeginLabel + ")", "(EMPTY)", "(EMPTY)");
            MidCodeList.add(Code.Op.LABEL, "(LABEL" + realWhileBeginLabel + ")", "(EMPTY)", "(EMPTY)");
        }
        stmtTravel(whileStmt.getStmt());
        if (MipsGenerator.optimize.get("JumpOptimize")) condTravel(whileStmt.getCond(), realWhileBeginLabel, true);
        else MidCodeList.add(Code.Op.JUMP, whileBeginLabel, "(EMPTY)", "(EMPTY)");
        // 空jump
        // MidCodeList.add(Code.Op.JUMP, (MipsGenerator.optimize.get("JumpOptimize")) ? "(LABEL" + label + ")":  whileBeginLabel, "(EMPTY)", "(EMPTY)");
        MidCodeList.add(Code.Op.LABEL, "(LABEL" + label + ")", "(EMPTY)", "(EMPTY)");

        whileBeginEndLabelStack.pop();
        whileBeginEndLabelStack.pop();

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
            return;
        }
        MidCodeList.add(Code.Op.GET_INT, "(EMPTY)", "(EMPTY)", lValTravel(inputStmt.getLVal(), false));
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
            return;
        }

        String nickname = lValTravel(assignStmt.getlVal(), false);
        String var = expTravel(assignStmt.getExp());
        if (!var.startsWith("(T") || nickname.endsWith("]")) MidCodeList.add(Code.Op.ASSIGN, var, "(EMPTY)", nickname);
        else {
            // System.out.println(MidCodeList.codes.get(MidCodeList.codes.size() - 1) + " " + nickname);
            MidCodeList.codes.get(MidCodeList.codes.size() - 1).clearRes(nickname);
        }
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
        if (cycLevel == 0) {
            ErrorTable.add(new Error(Error.Type.BREAK_CONTINUE_OUT_LOOP, loopStmt.getContrTK().getLine()));
            return;
        }
        if (loopStmt.getContrTK().getType() == Type.BREAKTK) MidCodeList.add(Code.Op.JUMP, whileBeginEndLabelStack.lastElement(), "(EMPTY)", "(EMPTY)");
        else MidCodeList.add(Code.Op.JUMP, whileBeginEndLabelStack.get(whileBeginEndLabelStack.size() - 2), "(EMPTY)", "(EMPTY)");
    }

    /*
    ----------------------------------------
    Error: l
     */
    private void outputStmtTravel(OutputStmt outputStmt) {
        ArrayList<Exp> exps = outputStmt.getExps();

        String fs = outputStmt.getsTK().getStrVal();
        fs = fs.substring(1, fs.length() - 1);
        int cnt = 0;
        for (int i = 0; i < fs.length(); ++i) {
            if (fs.charAt(i) == '%' && i + 1 < fs.length() && fs.charAt(i + 1) == 'd') ++cnt;
        }

        // l
        if (cnt != exps.size()) {
            ErrorTable.add(new Error(Error.Type.MISMATCH_PRINT_FORMAT_CHAR, outputStmt.getPrintTK().getLine()));
            return;
        }

        // 先算
        ArrayList<String> vals = new ArrayList<>();
        for (Exp exp : exps) {
            vals.add(expTravel(exp));
        }
        // if (vals.contains("(T91)")) System.out.println(vals);
        int j = 0;
        StringBuilder p = new StringBuilder();
        for (int i = 0; i < fs.length(); ++i) {
            if (fs.charAt(i) == '%' && i + 1 < fs.length() && fs.charAt(i + 1) == 'd') {
                if (p.length() != 0) MidCodeList.add(Code.Op.PRINT_STR, p.toString(), "(EMPTY)", "(EMPTY)");
                p = new StringBuilder();
                MidCodeList.add(Code.Op.PRINT_INT, vals.get(j++), "(EMPTY)", "(EMPTY)");
                ++i;
            } else p.append(fs.charAt(i));
        }
        if (p.length() != 0) MidCodeList.add(Code.Op.PRINT_STR, p.toString(), "(EMPTY)", "(EMPTY)");
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
        String x = null;
       if (returnStmt.getExp() != null) x = expTravel(returnStmt.getExp());
       MidCodeList.add(Code.Op.RETURN,  x != null ? x : "(EMPTY)", "(EMPTY)", "(EMPTY)");
    }

    // -------------------------- EXP ----------------------------

    // ok = false 不满足跳 label
    // ok = true 满足跳 label
    private void lOrExpTravel(LOrExp lOrExp, Integer label, boolean ok) {
        LAndExp first = lOrExp.getFirst();
        if (!ok) {
            // ... || ... || ... x
            // 1. 满足跳 x 不满足顺序
            // 2. 满足跳 x 不满足顺序
            // ...
            // n. 不满足跳 label
            Integer x = ++MidCodeList.labelCounter;
            // ... 1. 不满足跳 label
            // ... || ...... 1. 满足跳 x
            if (lOrExp.getTs().size() == 0) {
                lAndExpTravel(first, label, false);
                return;
            }
            lAndExpTravel(first, x, true);
            for (int i = 0; i < lOrExp.getTs().size(); ++i) {
                if (i != lOrExp.getTs().size() - 1) lAndExpTravel(lOrExp.getTs().get(i), x, true);
                else lAndExpTravel(lOrExp.getTs().get(i), label, false);
            }
            MidCodeList.add(Code.Op.LABEL, "(LABEL" + x + ")", "(EMPTY)", "(EMPTY)");
        } else {
            // ... || ... || ...
            // 满足跳 label
            lAndExpTravel(first, label, true);
            for (LAndExp exp : lOrExp.getTs()) lAndExpTravel(exp, label, true);
        }
    }

    /*
    ---------------------------------------------------------------
    Error: d, e
     */
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
            String param = expTravel(first);

            // e
            ArrayList<Symbol> symbols = new ArrayList<Symbol>(func.getFuncTable().getOrderSymbols().subList(0, func.getNum())); // 形参
            ArrayList<String> params = new ArrayList<>();
            ArrayList<Boolean> addr = new ArrayList<>();

            // System.out.println(symbols);
            int dim = first.getFormDim();
            if (dim != -10000 && dim != symbols.get(0).getDim()) {
                ErrorTable.add(new Error(Error.Type.MISMATCH_PARAM_TYPE, ident.getLine()));
                return;
            } else if (dim == -10000) {
                return;
            }
            //MidCodeList.add(symbols.get(0).getDim() != 0 ? Code.Op.PUSH_PAR_ADDR : Code.Op.PUSH_PAR_INT, param, "(EMPTY)", "(EMPTY)");
            params.add(param);
            addr.add(symbols.get(0).getDim() != 0);
            // System.out.println(funcRParams.getExps().size());

            for (int i = 0; i < funcRParams.getExps().size(); ++i) {
                Exp exp = funcRParams.getExps().get(i);
                param = expTravel(exp);

                // e
                dim = exp.getFormDim();
                //System.out.println(symbols + " " + symbols.get(i + 1) + " " + (i + 1));
                if (dim != -10000 && dim != symbols.get(i + 1).getDim()) {
                    ErrorTable.add(new Error(Error.Type.MISMATCH_PARAM_TYPE, ident.getLine()));
                    return;
                } else if (dim == -10000) {
                    return;
                }
                // MidCodeList.add(symbols.get(i + 1).getDim() != 0 ? Code.Op.PUSH_PAR_ADDR : Code.Op.PUSH_PAR_INT, param, "(EMPTY)", "(EMPTY)");
                params.add(param);
                addr.add(symbols.get(i + 1).getDim() != 0);
            }
            for (int i = 0; i < params.size(); ++i) {
                MidCodeList.add(addr.get(i) ? Code.Op.PUSH_PAR_ADDR : Code.Op.PUSH_PAR_INT, params.get(i), "(EMPTY)", "(EMPTY)");
            }

        } else {

            // d
            if (func.getNum() != 0) {
                ErrorTable.add(new Error(Error.Type.MISMATCH_PARAM_NUM, ident.getLine()));
            }
        }
    }

    private String primaryExpTravel(PrimaryExp primaryExp) {
        LVal lVal = primaryExp.getLval();
        Exp exp = primaryExp.getExp();

        if (exp != null) {
            return expTravel(exp);
        } else if (lVal != null) {
            return lValTravel(lVal, true);
        }
        // number
        return Integer.toString(primaryExp.getNumber().val());
    }

    /*
    函数调用只占一行
    -------------------------------------
    Error: c, d, e
     */
    private String unaryExpTravel(UnaryExp unaryExp) {
        PrimaryExp primaryExp = unaryExp.getPrimaryExp();
        UnaryExp innerUnaryExp = unaryExp.getUnaryExp();
        Token ident = unaryExp.getIdentTk();

        if (primaryExp != null) {
            return primaryExpTravel(primaryExp);
        } else if (innerUnaryExp != null) {
            String ord = unaryExpTravel(innerUnaryExp);
            Token op = unaryExp.getUnaryOp();
            try {
                Integer x = Integer.parseInt(ord);
                return op.getType() == Type.MINU ? Integer.toString(-x) :
                                        op.getType() == Type.PLUS ? Integer.toString(x) : (x == 0 ? "1" : "0");
            } catch (Exception ignore) {}
            if (op.getType() != Type.NOT) return MidCodeList.add(op.getType() == Type.MINU ? Code.Op.SUB : Code.Op.ADD, "0", ord, "(AUTO)");
            else return MidCodeList.add(Code.Op.NOT, ord, "(EMPTY)", "(AUTO)");
        } else {

            // 检查函数调用
            assert ident != null;
            String name = ident.getStrVal();

            // c
            if (!curTable.contains(name, true)) {
                ErrorTable.add(new Error(Error.Type.UNDEFINE_IDENT, ident.getLine()));
                return "(ERROR)";
            }

            MidCodeList.add(Code.Op.PREPARE_CALL, curTable.get(name, true).getNickname(), "(EMPTY)", "(EMPTY)");
            FuncRParams funcRParams = unaryExp.getFuncRParams();
            funcRParamsTravel(funcRParams, ident, name);
            return MidCodeList.add(Code.Op.CALL, curTable.get(name, true).getNickname(), "(EMPTY)", "(EMPTY)");
        }
    }

    private String mulExpTravel(MulExp mulExp) {
        UnaryExp first = mulExp.getFirst();
        String ord1 = unaryExpTravel(first);
        String res = ord1;
        for (int i = 0; i < mulExp.getTs().size(); ++i) {
            String ord2 = unaryExpTravel(mulExp.getTs().get(i));
            try {
                Integer x = Integer.parseInt(ord1);
                Integer y = Integer.parseInt(ord2);
                res = mulExp.getOperators().get(i).getType() == Type.MULT ? Integer.toString(x * y) :
                        mulExp.getOperators().get(i).getType() == Type.DIV ? Integer.toString(x / y) : Integer.toString(x % y);
            } catch (Exception ignore) {
                Code.Op op = mulExp.getOperators().get(i).getType() == Type.MULT ? Code.Op.MUL :
                        mulExp.getOperators().get(i).getType() == Type.DIV ? Code.Op.DIV : Code.Op.MOD;
                res = MidCodeList.add(op, ord1, ord2, "(AUTO)");
            }
            ord1 = res;
        }
        return res;
    }

    private String addExpTravel(AddExp addExp) {
        MulExp first = addExp.getFirst();
        String ord1 = mulExpTravel(first);
        String res = ord1;
        for (int i = 0; i < addExp.getTs().size(); ++i) {
            String ord2 = mulExpTravel(addExp.getTs().get(i));
            try {
                Integer x = Integer.parseInt(ord1);
                Integer y = Integer.parseInt(ord2);
                res = addExp.getOperators().get(i).getType() == Type.PLUS ? Integer.toString(x + y) : Integer.toString(x - y);
            } catch (Exception ignore) {
                Code.Op op = addExp.getOperators().get(i).getType() == Type.PLUS ? Code.Op.ADD : Code.Op.SUB;
                res = MidCodeList.add(op, ord1, ord2, "(AUTO)");
            }
            ord1 = res;
        }
        return res;
    }

    private String relExpTravel(RelExp relExp) {
        AddExp first = relExp.getFirst();
        String ord1 = addExpTravel(first);
        String res = ord1;
        for (int i = 0; i <  relExp.getTs().size(); ++i) {
            String ord2 = addExpTravel(relExp.getTs().get(i));
            try {
                int x = Integer.parseInt(ord1);
                int y = Integer.parseInt(ord2);
                res = relExp.getOperators().get(i).getType() == Type.LSS ? (x < y ? "1" : "0") :
                        relExp.getOperators().get(i).getType() == Type.LEQ ? (x <= y ? "1" : "0") :
                                relExp.getOperators().get(i).getType() == Type.GRE ? (x > y ? "1" : "0") : (x >= y ? "1" : "0");
            } catch (Exception ignore) {
                Code.Op op = relExp.getOperators().get(i).getType() == Type.GRE ? Code.Op.GT :
                        relExp.getOperators().get(i).getType() == Type.LSS ? Code.Op.LT :
                                relExp.getOperators().get(i).getType() == Type.GEQ ? Code.Op.GE : Code.Op.LE;
                res = MidCodeList.add(op, ord1, ord2, "(AUTO)");
            }
            ord1 = res;
        }
        return res;
    }

    // 满足跳 x, ok = true
    // 不满足跳 x, ok = false
    // == 和 && 规则相似
    private void eqExpTravel(EqExp eqExp, Integer x, boolean ok) {
        RelExp first = eqExp.getFirst();
        String ord1 = relExpTravel(first);
        String res = ord1;
        for (int i = 0; i <  eqExp.getTs().size(); ++i) {
            String ord2 = relExpTravel(eqExp.getTs().get(i));
            try {
                int z = Integer.parseInt(ord1);
                int y = Integer.parseInt(ord2);
                res = eqExp.getOperators().get(i).getType() == Type.EQL ? (z == y ? "1" : "0") : (z != y ? "1" : "0");
            } catch (Exception ignore) {
                Code.Op op = eqExp.getOperators().get(i).getType() == Type.EQL ? Code.Op.EQ : Code.Op.NE;
                res = MidCodeList.add(op, ord1, ord2, "(AUTO)");
            }
            ord1 = res;
        }
        if (ok) {
            try {
                if (Integer.parseInt(res) != 0) MidCodeList.add(Code.Op.JUMP, "(LABEL" + x + ")", "(EMPTY)", "(EMPTY)");
            } catch (Exception ignore) {
                MidCodeList.add(Code.Op.NEZ_JUMP, res, "(EMPTY)", "(LABEL" + x + ")");
            }
        } else {
            try {
                if (Integer.parseInt(res) == 0) MidCodeList.add(Code.Op.JUMP, "(LABEL" + x + ")", "(EMPTY)", "(EMPTY)");
            } catch (Exception ignore) {
                MidCodeList.add(Code.Op.EQZ_JUMP, res, "(EMPTY)", "(LABEL" + x + ")");
            }
        }
    }

    // 满足跳 x, ok = true
    // 不满足跳 x, ok = false
    private void lAndExpTravel(LAndExp lAndExp, Integer x, boolean ok) {
        EqExp first = lAndExp.getFirst();
        ArrayList<EqExp> eqExps = lAndExp.getTs();
        if (ok) {
            // ... && ... && ... y
            // 前面的不满足跳 y
            // 最后一条满足跳 x
            Integer y = ++MidCodeList.labelCounter;
            if (eqExps.size() == 0) {
                eqExpTravel(first, x, true);
                return;
            }
            eqExpTravel(first, y, false);
            for (int i = 0; i < eqExps.size(); ++i) {
                if (i != eqExps.size() -1) eqExpTravel(eqExps.get(i), y, false);
                else eqExpTravel(eqExps.get(i), x, true);
            }
            MidCodeList.add(Code.Op.LABEL, "(LABEL" + y + ")", "(EMPTY)", "(EMPTY)");
        } else {
            // ... && ... && ...
            // 不满足跳 x
            eqExpTravel(first, x, false);
            for (EqExp exp : eqExps) eqExpTravel(exp, x, false);
        }
    }

    private void condTravel(Cond cond, Integer label, boolean ok) {
        lOrExpTravel(cond.getlOrExp(), label, ok);
    }

    private String expTravel(Exp exp) {
        //if (exp instanceof ConstExp || blockLevel == 0) { // 全局或常数
        if (MipsGenerator.optimize.get("MidCodeOptimize") || exp instanceof ConstExp || blockLevel == 0) { // 全局或常数
            try {
                return String.valueOf(Calc.calcExp(exp));
            } catch (Exception ignore) {
            }
            // }
        }
        return addExpTravel(exp.getAddExp());
    }

    /*
    ------------------------------------
    Error: c
     */
    private String lValTravel(LVal lVal, boolean assign) {
        Token ident = lVal.getIdentTk();
        String name = ident.getStrVal();

        // c
        if (!curTable.contains(name, true)) {
            ErrorTable.add(new Error(Error.Type.UNDEFINE_IDENT, ident.getLine()));
            return "(ERROR)";
        }

        Symbol symbol = curTable.get(name, true);
        String nickname = symbol.getNickname();
        ArrayList<Integer> dims = symbol instanceof Val ? ((Val) symbol).getDims() : ((FuncFormParam) symbol).getDims();

        ArrayList<Index> indexes = lVal.getIndexes();
        if (!indexes.isEmpty()) {
            String x = expTravel(indexes.get(0).getExp());
            // 有2维-->值
            // a[2][3]
            if (indexes.size() == 2) {
                String y = expTravel(indexes.get(1).getExp());
                String base;

                try {
                    base = String.valueOf(Integer.parseInt(x) * Integer.parseInt(dims.get(1).toString()));
                } catch (Exception ignore) {
                    base = MidCodeList.add(Code.Op.MUL, x, dims.get(1).toString(), "(AUTO)");
                }
                try {
                    nickname += "[" + (Integer.parseInt(y) + Integer.parseInt(base)) + "]";
                } catch (Exception ignore) {
                    nickname += "[" + MidCodeList.add(Code.Op.ADD, y, base, "(AUTO)") + "]";
                }

            } else {
                // 一维
                // 形参： a[], a[][3] ---> a[2]
                // 正常定义：a[3], a[2][3] ---> a[1]
                if (dims.size() != indexes.size()) { // 一维数组地址
                    try {
                        x = String.valueOf(Integer.parseInt(x) * Integer.parseInt(dims.get(1).toString()));
                    } catch (Exception ignore) {
                        x = MidCodeList.add(Code.Op.MUL, x, dims.get(1).toString(), "(AUTO)");
                    }
                }
                nickname += "[" + x + "]";
            }
        } else if (!dims.isEmpty()) { // 地址
            nickname += "[0]";
        }
        // 不为函数参数
        if (assign && dims.size() == indexes.size() && indexes.size() != 0) return MidCodeList.add(Code.Op.ASSIGN, nickname, "(EMPTY)", "(AUTO)");
        return nickname;
    }
}
