package Syntax;

import Error.Error;
import Lexer.Token;
import Lexer.Type;
import Syntax.Decl.Decl;
import Syntax.Decl.Def;
import Syntax.Decl.InitVal.InitArr;
import Syntax.Decl.InitVal.InitExp;
import Syntax.Decl.InitVal.InitVal;
import Syntax.Expr.Multi.*;
import Syntax.Expr.Unary.FuncRParams;
import Syntax.Expr.Unary.LVal;
import Syntax.Expr.Unary.Number;
import Syntax.Expr.Unary.PrimaryExp;
import Syntax.Expr.Unary.UnaryExp;
import Syntax.Func.*;
import Syntax.Stmt.BlockStmt;
import Syntax.Stmt.IfStmt;
import Syntax.Stmt.Simple.*;
import Syntax.Stmt.Stmt;
import Syntax.Stmt.WhileStmt;
import Syntax.Util.Index;
import Error.ErrorTable;

import java.util.ArrayList;

public class Parser {
    private final ArrayList<Token> tokens;
    private final CompUnit compUnit;
    private Token now = null;
    private Token pre = null;
    private Token nxt = null;
    private int pos = 0;

    public Parser(ArrayList<Token> tokens) {
        this.tokens = tokens;
        peek();
        compUnit = parseCompUnit();
    }

    public CompUnit getCompUnit() {
        return compUnit;
    }

    private void deBug() {
        System.out.println(pre + " " + now + " " + nxt);
    }

    @Override
    public String toString() {
        return compUnit.toString();
    }

    private void peek() {
        pre = (0 <= pos - 1 && pos - 1 < tokens.size()) ? tokens.get(pos - 1) : null;
        now = (0 <= pos && pos < tokens.size()) ? tokens.get(pos++) : null;
        nxt = (0 <= pos && pos < tokens.size()) ? tokens.get(pos) : null;
    }

    private void insert(Token token) {
        retract(1);
        tokens.add(pos, token);
        peek();
    }

    private void retract(int step) {
        pos -= (step + 1);
        if (pos < 0) {
            System.out.println("retract error!");
            System.exit(-1);
        }
        peek();
    }
    
    private Type getType(Token token) {
        return token != null ? token.getType() : null;
    }

    /*
    CompUnit -> {Decl} {FuncDef} MainFuncDef
    ----------------------------------------
    Decl: "const int" Ident ...";" | "int" Ident ...";"
    FuncDef: ("void" | "int") Ident "(" ..."}"
    MainFuncDef: "int" "main" "("
     */
    public CompUnit parseCompUnit() {
        CompUnit compUnit = new CompUnit();
        while (now != null) {
            peek();
            if (getType(pre) == Type.CONSTTK || (getType(pre) == Type.INTTK
                    && getType(now) == Type.IDENFR && getType(nxt) != Type.LPARENT)) {
                retract(1);
                compUnit.addDecl(parseDecl());
            } else if ((getType(pre) == Type.VOIDTK || getType(pre) == Type.INTTK)
                    && getType(now) == Type.IDENFR && getType(nxt) == Type.LPARENT) {
                retract(1);
                compUnit.addFuncDef(parseFuncDef());
            } else if (getType(pre) == Type.INTTK && getType(now) == Type.MAINTK) {
                retract(1);
                compUnit.setMainFuncDef(parseMainFuncDef());
            } else {
                System.out.println("parseCompUnit error!");
                System.exit(-1);
            }
        }
        return compUnit;
    }

    // -------------------------------------------------- DECL ---------------------------------------------------------

    /*
    Decl -> ConstDecl | VarDecl
    ---------------------------
    ConstDecl: "const"...";"
    VarDecl: "int"...";"
     */
    public Decl parseDecl() {
        if (getType(now) == Type.CONSTTK) return parseConstDecl();
        else if (getType(now) == Type.INTTK) return parseVarDecl();
        else {
            System.out.println("parseDecl error!");
            System.exit(-1);
        }
        return null;
    }

    /*
    ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
    ------------------------------------------------------------
    Error: i
     */
    public Decl parseConstDecl() {
        peek();
        Decl constDecl = new Decl(pre, now);
        while (getType(now) != Type.SEMICN) {
            peek();
            constDecl.addDef(parseConstDef());
            if (getType(now) == Type.COMMA) constDecl.addComma(now);
            else if (getType(now) == Type.SEMICN) constDecl.setSemicolonTk(now);
            else {
                // i
                insert(new Token(Type.SEMICN, pre.getLine()));
                constDecl.setSemicolonTk(now);
                ErrorTable.add(new Error(Error.Type.LACK_SEMICOLON, pre.getLine()));
            }
        }
        peek();
        return constDecl;
    }

    /*
    ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    --------------------------------------------------------
    Error: k
     */
    public Def parseConstDef() {
        Def constDef = new Def(true, now);
        peek();
        while (getType(now) == Type.LBRACK) {
            Index idx  = new Index(now);
            peek();
            idx.setExp(parseConstExp());
            if (getType(now) != Type.RBRACK) {
                insert(new Token(Type.RBRACK, pre.getLine()));
                ErrorTable.add(new Error(Error.Type.LACK_RIGHT_BRACKET, pre.getLine()));
            }
            idx.setRBTk(now);
            constDef.addIndex(idx);
            peek();
        }
        constDef.setAssignTk(now);
        peek();
        constDef.setInitVal(parseConstInitVal());
        return constDef;
    }

    /*
    ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    ------------------------------------------------------------------------
    ConstExp: Ident ... | "(" ... | IntConst | "+" | "-" | "!"
     */
    public InitVal parseConstInitVal() {
        InitVal initVal;
        if (getType(now) != Type.LBRACE) {
            initVal = new InitExp(parseConstExp(), true);
            return initVal;
        }
        initVal = new InitArr(now, true);
        peek();
        while (getType(now) != Type.RBRACE) {
            ((InitArr) initVal).addVar(parseConstInitVal());
            if (getType(now) == Type.COMMA) {
                ((InitArr) initVal).addComma(now);
                peek();
            } else if (getType(now) != Type.RBRACE) {
                System.out.println("parseConstInitVal error!");
                System.exit(-1);
            }
        }
        ((InitArr) initVal).setRBTK(now);
        peek();
        return initVal;
    }

    /*
    VarDecl -> BType VarDef { ',' VarDef } ';'
    ------------------------------------------
    Error: i
     */
    public Decl parseVarDecl() {
        Decl varDecl = new Decl(null, now);
        while (getType(now) != Type.SEMICN) {
            peek();
            varDecl.addDef(parseVarDef());
            if (getType(now) == Type.COMMA) varDecl.addComma(now);
            else if (getType(now) == Type.SEMICN) varDecl.setSemicolonTk(now);
            else {
                // i
                insert(new Token(Type.SEMICN, pre.getLine()));
                varDecl.setSemicolonTk(now);
                ErrorTable.add(new Error(Error.Type.LACK_SEMICOLON, pre.getLine()));
            }
        }
        peek();
        return varDecl;
    }

    /*
    VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    -----------------------------------------------------------------------------
    VarDef -> Ident { '[' ConstExp ']' } [ '=' InitVal ]
    -----------------------------------------------------------
    Error: k
     */
    public Def parseVarDef() {
        Def varDef = new Def(false, now);
        peek();
        while (getType(now) == Type.LBRACK) {
            Index idx  = new Index(now);
            peek();
            idx.setExp(parseConstExp());
            if (getType(now) != Type.RBRACK) {
                insert(new Token(Type.RBRACK, pre.getLine()));
                ErrorTable.add(new Error(Error.Type.LACK_RIGHT_BRACKET, pre.getLine()));
            }
            idx.setRBTk(now);
            varDef.addIndex(idx);
            peek();
        }
        if (getType(now) == Type.ASSIGN) {
            varDef.setAssignTk(now);
            peek();
            varDef.setInitVal(parseInitVal());
        }
        return varDef;
    }

    /*
    InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    ----------------------------------------------------------
    Exp: Ident ... | "(" ... | IntConst | "+" | "-" | "!"
     */
    public InitVal parseInitVal() {
        InitVal initVal;
        if (getType(now) == Type.IDENFR || getType(now) == Type.LPARENT || getType(now) == Type.INTCON
                || getType(now) == Type.PLUS || getType(now) == Type.MINU || getType(now) == Type.NOT) {
            initVal = new InitExp(parseExp(), false);
            return initVal;
        }
        initVal = new InitArr(now, false);
        peek();
        while (getType(now) != Type.RBRACE) {
            ((InitArr) initVal).addVar(parseInitVal());
            if (getType(now) == Type.COMMA) ((InitArr) initVal).addComma(now);
            else if (getType(now) == Type.RBRACE) break;
            else {
                System.out.println("parseInitVal error!");
                System.exit(-1);
            }
            peek();
        }
        ((InitArr) initVal).setRBTK(now);
        peek();
        return initVal;
    }

    // -------------------------------------------------- FUNC ---------------------------------------------------------

    /*
    FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    -------------------------------------------------------
    FuncType: "void" | "int"
    FuncFParams: "int"
    ----------------------------------------------------------
    Error: j
     */
    public FuncDef parseFuncDef() {
        peek();
        FuncDef funcDef = new FuncDef(pre, now);
        peek();
        if (getType(now) == Type.LPARENT) funcDef.setLPTK(now);
        else {
            System.out.println("parseFuncDef error!");
            System.exit(-1);
        }
        peek();
        if (getType(now) == Type.INTTK) funcDef.setFuncFParams(parseFuncFParams());
        if (getType(now) == Type.RPARENT) funcDef.setRPTK(now);
        else {
            insert(new Token(Type.RPARENT, pre.getLine()));
            funcDef.setRPTK(now);
            ErrorTable.add(new Error(Error.Type.LACK_RIGHT_PARENT, pre.getLine()));
        }
        peek();
        funcDef.setBlock(parseBlock());
        return funcDef;
    }

    /*
    FuncFParams -> FuncFParam { ',' FuncFParam }
     */
    public FuncFParams parseFuncFParams() {
        FuncFParams funcFParams = new FuncFParams(parseFuncFParam());
        // deBug();
        while (getType(now) == Type.COMMA) {
            funcFParams.addComma(now);
            peek();
            funcFParams.addFuncFParam(parseFuncFParam());
        }
        return funcFParams;
    }

    /*
    FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]
    BType: "int"
    ConstExp: Ident ... | "(" ... | IntConst | "+" | "-" | "!"
    --------------------------------------------------------------
    Error: k
     */
    public FuncFParam parseFuncFParam() {
        peek();
        FuncFParam funcFParam = new FuncFParam(pre, now);
        peek();
        while (getType(now) == Type.LBRACK) {
            Index idx = new Index(now);
            peek();
            if (getType(now) == Type.IDENFR || getType(now) == Type.LPARENT || getType(now) == Type.INTCON
                    || getType(now) == Type.PLUS || getType(now) == Type.MINU || getType(now) == Type.NOT)
                idx.setExp(parseConstExp());
            if (getType(now) != Type.RBRACK) {
                insert(new Token(Type.RBRACK, pre.getLine()));
                ErrorTable.add(new Error(Error.Type.LACK_RIGHT_BRACKET, pre.getLine()));
            }
            idx.setRBTk(now);
            funcFParam.addIndex(idx);
            peek();
        }
        return funcFParam;
    }

    // --------------------------------------------------- MAIN --------------------------------------------------------

    /*
    MainFuncDef -> 'int' 'main' '(' ')' Block
    ----------------------------------------------
    Error: j
     */
    public MainFuncDef parseMainFuncDef() {
        peek();
        MainFuncDef mainfuncDef = new MainFuncDef(pre, now);
        peek();
        if (getType(now) == Type.LPARENT) mainfuncDef.setLPTK(now);
        else {
            System.out.println("parseMainFuncDef error!");
            System.exit(-1);
        }
        peek();
        if (getType(now) == Type.RPARENT) mainfuncDef.setRPTK(now);
        else {
            insert(new Token(Type.RPARENT, pre.getLine()));
            mainfuncDef.setRPTK(now);
            ErrorTable.add(new Error(Error.Type.LACK_RIGHT_PARENT, pre.getLine()));
        }
        peek();
        mainfuncDef.setBlock(parseBlock());
        return mainfuncDef;
    }

    /*
    Block -> '{' { BlockItem } '}'
     */
    public Block parseBlock() {
        Block block = new Block(now);
        peek();
        while (getType(now) != Type.RBRACE) block.addBlockItem(parseBlockItem());
        block.setRBTK(now);
        peek();
        return block;
    }

    /*
    BlockItem -> Decl | Stmt
    -------------------------
    Decl: "const int" Ident ...";" | "int" Ident ...";"
    Stmt:
     */
    public Block.BlockItem parseBlockItem() {
        if (getType(now) == Type.CONSTTK || getType(now) == Type.INTTK) return new Block.BlockItem(parseDecl());
        else return new Block.BlockItem(parseStmt());
    }

    /*
    Stmt -> LVal '=' Exp ';'
    | [Exp] ';'
    | Block
    | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    | 'while' '(' Cond ')' Stmt
    | 'break' ';' | 'continue' ';'
    | 'return' [Exp] ';'
    | LVal '=' 'getint''('')'';'
    | 'printf''('FormatString{','Exp}')'';'
    --------------------------------------------------
    <AssignStmt> -> LVal '=' Exp
    <ExpStmt> -> Exp
    <LoopStmt> -> 'break' | 'continue'
    <ReturnStmt> -> 'return' [Exp]
    <InputStmt> -> <LVal> '=' 'getint' '(' ')'
    <OutputStmt> ->  'printf''('FormatString{','Exp}')'

    <SimpleStmt> -> [ <AssignStmt> | <ExpStmt> | <LoopStmt> | <ReturnStmt> |
                    <InputStmt> | <OutputStmt> ] ';'
    <IfStmt> -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    <WhileStmt> -> 'while' '(' Cond ')' Stmt
    <BlockStmt> -> Block

    <Stmt> -> <SimpleStmt> | <IfStmt> | <WhileStmt> | <BlockStmt>
    ----------------------------------------------------
    Block: "{"....
    LVal : Ident {'[' Exp ']'}
    Exp: LVal | "(" Exp ")" | IntConst | Ident "(" ... | "+" | "-" | "!"
    故如果仅有一个 LVal 且 LVal 后面跟 "=" 那么为第一种情况
     */
    public Stmt parseStmt() {
        if (getType(now) == Type.IFTK) return new Stmt(parseIfStmt());
        else if (getType(now) == Type.WHILETK) return new Stmt(parseWhileStmt());
        else if (getType(now) == Type.LBRACE) return new Stmt(new BlockStmt(parseBlock()));
        else return new Stmt(parseSimpleStmt());
    }

    /*
    <SimpleStmt> -> [ <AssignStmt> | <ExpStmt> | <LoopStmt> | <ReturnStmt> |
                    <InputStmt> | <OutputStmt> ] ';'
     -------------------------------------------------------------------------
     AssignStmt: LVal "=" Exp
     ExpStmt:   Exp
     LoopStmt: "break" | "continue"
     ReturnStmt: "return"
     InputStmt: LVal "=" "getint"
     OutputStmt: "printf"
     ---------------------------------------------------------------------------
      LVal : Ident {'[' Exp ']'}
      Exp: LVal | "(" Exp ")" | IntConst | Ident "(" ... | "+" | "-" | "!"
      ---------------------------------------------------------------------------
      Error: i
     */
    public SimpleStmt parseSimpleStmt() {
        SimpleStmt simpleStmt = null;
        if (getType(now) == Type.BREAKTK || getType(now) == Type.CONTINUETK) {
            simpleStmt = new SimpleStmt(parseLoopStmt());
        } else if (getType(now) == Type.RETURNTK) {
            simpleStmt = new SimpleStmt(parseReturnStmt());
        } else if (getType(now) == Type.PRINTFTK) {
            simpleStmt = new SimpleStmt(parseOutputStmt());
        } else if (getType(now) == Type.LPARENT || getType(now) == Type.INTCON || getType(now) == Type.MINU
                || getType(now) == Type.PLUS || getType(now) == Type.NOT
                || (getType(now) == Type.IDENFR && getType(nxt) == Type.LPARENT)) {
            simpleStmt = new SimpleStmt(parseExpStmt());
        } else if (getType(now) != Type.SEMICN) {
            int prePos = pos - 1; // now_pos + 1
            // System.out.println(prePos + " " + pre + " " + now + " " + nxt);
            parseLVal();
            // System.out.println(pos + " " + pre + " " + now + " " + nxt);
            Token tmpNow = now, tmpNxt = nxt;
            retract((pos - 1) - prePos);
            if (getType(tmpNow) == Type.ASSIGN) {
                if (getType(tmpNxt) == Type.GETINTTK) simpleStmt = new SimpleStmt(parseInputStmt());
                else simpleStmt = new SimpleStmt(parseAssignStmt());
            } else {
                simpleStmt = new SimpleStmt(parseExpStmt());
            }
        }
        if (getType(now) == Type.SEMICN) {
            if (simpleStmt != null) simpleStmt.setSemicolonTk(now);
            else simpleStmt = new SimpleStmt(now);
            peek();
        } else {
            // i
            insert(new Token(Type.SEMICN, pre.getLine()));
            assert simpleStmt != null;
            simpleStmt.setSemicolonTk(now);
            ErrorTable.add(new Error(Error.Type.LACK_SEMICOLON, pre.getLine()));
            peek();
            // deBug();
        }
        return simpleStmt;
    }

    /*
    <IfStmt> -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    -------------------------------------------------------
    Error: j
     */
    public IfStmt parseIfStmt() {
        IfStmt ifStmt = new IfStmt(now);
        peek();
        ifStmt.setLPTK(now);
        peek();
        ifStmt.setCond(parseCond());
        if (getType(now) != Type.RPARENT) {
            insert(new Token(Type.RPARENT, pre.getLine()));
            ErrorTable.add(new Error(Error.Type.LACK_RIGHT_PARENT, pre.getLine()));
        }
        ifStmt.setRPTK(now);
        peek();
        ifStmt.setIfStmt(parseStmt());
        if (getType(now) == Type.ELSETK) {
            ifStmt.setElseTK(now);
            peek();
            ifStmt.setElseStmt(parseStmt());
        }
        return ifStmt;
    }

    /*
    <WhileStmt> -> 'while' '(' Cond ')' Stmt
    ----------------------------------------
    Error: j
     */
    public WhileStmt parseWhileStmt() {
        WhileStmt whileStmt = new WhileStmt(now);
        peek();
        whileStmt.setLPTK(now);
        peek();
        whileStmt.setCond(parseCond());
        if (getType(now) != Type.RPARENT) {
            insert(new Token(Type.RPARENT, pre.getLine()));
            ErrorTable.add(new Error(Error.Type.LACK_RIGHT_PARENT, pre.getLine()));
        }
        whileStmt.setRPTK(now);
        peek();
        whileStmt.setStmt(parseStmt());
        return whileStmt;
    }

    /*
    <AssignStmt> -> LVal '=' Exp
     */
    public AssignStmt parseAssignStmt() {
        AssignStmt assignStmt = new AssignStmt(parseLVal());
        assignStmt.setAssignTK(now);
        peek();
        assignStmt.setExp(parseExp());
        return assignStmt;
    }

    /*
    <ExpStmt> -> Exp
     */
    public ExpStmt parseExpStmt() {
        return new ExpStmt(parseExp());
    }

    /*
    <LoopStmt> -> 'break' | 'continue'
     */
    public LoopStmt parseLoopStmt() {
        LoopStmt loopStmt = new LoopStmt(now);
        peek();
        return loopStmt;
    }

    /*
    <ReturnStmt> -> 'return' [Exp]
    -------------------------------------
    Exp: Ident ... | "(" ... | IntConst | "+" | "-" | "!"
     */
    public ReturnStmt parseReturnStmt() {
        ReturnStmt returnStmt = new ReturnStmt(now);
        peek();
        if (getType(now) == Type.IDENFR || getType(now) == Type.LPARENT || getType(now) == Type.INTCON
        || getType(now) == Type.PLUS || getType(now) == Type.MINU || getType(now) == Type.NOT) returnStmt.setExp(parseExp());
        return returnStmt;
    }

    /*
    <InputStmt> -> <LVal> '=' 'getint' '(' ')'
    ---------------------------------------------
    Error: j
     */
    public InputStmt parseInputStmt() {
        InputStmt inputStmt = new InputStmt(parseLVal());
        inputStmt.setAssignTK(now);
        peek();
        inputStmt.setGetintTK(now);
        peek();
        inputStmt.setLPTK(now);
        peek();
        if (getType(now) != Type.RPARENT) {
            insert(new Token(Type.RPARENT, pre.getLine()));
            ErrorTable.add(new Error(Error.Type.LACK_RIGHT_PARENT, pre.getLine()));
        }
        inputStmt.setRPTK(now);
        peek();
        return inputStmt;
    }

    /*
    <OutputStmt> ->  'printf''('FormatString{','Exp}')'
    ---------------------------------------------------
    Error: j
     */
    public OutputStmt parseOutputStmt() {
        OutputStmt outputStmt = new OutputStmt(now);
        peek();
        outputStmt.setLPTK(now);
        peek();
        outputStmt.setSTK(now);
        peek();
        while (getType(now) == Type.COMMA) {
            outputStmt.addComma(now);
            peek();
            outputStmt.addExp(parseExp());
        }
        if (getType(now) != Type.RPARENT) {
            insert(new Token(Type.RPARENT, pre.getLine()));
            ErrorTable.add(new Error(Error.Type.LACK_RIGHT_PARENT, pre.getLine()));
        }
        outputStmt.setRPTK(now);
        peek();
        return outputStmt;
    }

    // --------------------------------------------------- EXP ---------------------------------------------------------

    /*
    Exp -> AddExp
     */
    public Exp parseExp() {
        return new Exp(parseAddExp());
    }

    /*
    Cond -> LOrExp
     */
    public Cond parseCond() {
        return new Cond(parseLOrExp());
    }

    /*
    LVal -> Ident {'[' Exp ']'}
    ----------------------------
    Error: k
     */
    public LVal parseLVal() {
        LVal lVal = new LVal(now);
        peek();
        while (getType(now) == Type.LBRACK) {
            Index index = new Index(now);
            peek();
            index.setExp(parseExp());
            if (getType(now) != Type.RBRACK) {
                insert(new Token(Type.RBRACK, pre.getLine()));
                ErrorTable.add(new Error(Error.Type.LACK_RIGHT_BRACKET, pre.getLine()));
            }
            index.setRBTk(now);
            lVal.addIndex(index);
            peek();
        }
        return lVal;
    }

    /*
    PrimaryExp ->  '(' Exp ')' | LVal | Number
    ------------------------------------------
    LVal: Ident { "[" Exp "]" }
    Number: IntConst
     */
    public PrimaryExp parsePrimaryExp() {
        if (getType(now) == Type.LPARENT) {
            PrimaryExp primaryExp = new PrimaryExp(now);
            peek();
            primaryExp.setExp(parseExp());
            if (getType(now) == Type.RPARENT) {
                primaryExp.setRPTk(now);
                peek();
                return primaryExp;
            } else {
                System.out.println("parsePrimaryExp error!");
                System.exit(-1);
            }
        }
        if (getType(now) == Type.IDENFR) {
            return new PrimaryExp(parseLVal());
        } else if (getType(now) == Type.INTCON) {
            peek();
            return new PrimaryExp(new Number(pre));
        } else {
            // deBug();
            System.out.println("parsePrimaryExp error!");
            System.exit(-1);
        }
        return null;
    }

    /*
    UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    -----------------------------------------------------------------------
    PrimaryExp: Ident (null | "[" ...) | "(" ... | IntConst
    UnaryOp: "+" | "-" | "!"
    FuncRParams: Ident ... | "(" ... | IntConst | "+" | "-" | "!"
    ---------------------------------------------------------------------
    Error: j
     */
    public UnaryExp parseUnaryExp() {
        UnaryExp unaryExp;
        if (getType(now) == Type.MINU ||getType(now) == Type.PLUS ||getType(now) == Type.NOT) {
            unaryExp = new UnaryExp(now);
            peek();
            unaryExp.setUnaryExp(parseUnaryExp());
            return unaryExp;
        } else if (getType(now) == Type.IDENFR && getType(nxt) == Type.LPARENT) {
            unaryExp = new UnaryExp(now);
            peek();
            unaryExp.setLPTk(now);
            peek();
            if (getType(now) == Type.IDENFR || getType(now) == Type.LPARENT || getType(now) == Type.INTCON
                    || getType(now) == Type.PLUS || getType(now) == Type.MINU || getType(now) == Type.NOT)
                unaryExp.setFuncRParams(parseFuncRParams());
            if (getType(now) == Type.RPARENT) {
                unaryExp.setRPTk(now);
            } else {
                insert(new Token(Type.RPARENT, pre.getLine()));
                unaryExp.setRPTk(now);
                ErrorTable.add(new Error(Error.Type.LACK_RIGHT_PARENT, pre.getLine()));
            }
            peek();
            return unaryExp;
        } else {
            unaryExp = new UnaryExp(parsePrimaryExp());
            return unaryExp;
        }
    }

    /*
    FuncRParams -> Exp { ',' Exp }
     */
    public FuncRParams parseFuncRParams() {
        FuncRParams funcRParams = new FuncRParams(parseExp());
        while (getType(now) == Type.COMMA) {
            funcRParams.addComma(now);
            peek();
            funcRParams.addExp(parseExp());
        }
        return funcRParams;
    }

    /*
    MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    -------------------------------------------------------
    MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
     */
    public MulExp parseMulExp() {
        MulExp mulExp = new MulExp(parseUnaryExp());
        while (getType(now) == Type.MULT ||getType(now) == Type.DIV || getType(now) == Type.MOD || getType(now) == Type.BITAND) {
            mulExp.addOperator(now);
            peek();
            mulExp.addT(parseUnaryExp());
        }
        return mulExp;
    }

    /*
    AddExp -> MulExp | AddExp ('+' | '−') MulExp
    --------------------------------------------
    AddExp -> MulExp { ('+' | '−') MulExp }
     */
    public AddExp parseAddExp() {
        AddExp addExp = new AddExp(parseMulExp());
        while (getType(now) == Type.MINU ||getType(now) == Type.PLUS) {
            addExp.addOperator(now);
            peek();
            addExp.addT(parseMulExp());
        }
        return addExp;
    }

    /*
    RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    ----------------------------------------------------------
    RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp }
     */
    public RelExp parseRelExp() {
        RelExp relExp = new RelExp(parseAddExp());
        while (getType(now) == Type.LSS ||getType(now) == Type.LEQ
                || getType(now) == Type.GRE || getType(now) == Type.GEQ) {
            relExp.addOperator(now);
            peek();
            relExp.addT(parseAddExp());
        }
        return relExp;
    }

    /*
    EqExp -> RelExp | EqExp ('==' | '!=') RelExp
    --------------------------------------------
    EqExp -> RelExp { ('==' | '!=') RelExp }
     */
    public EqExp parseEqExp() {
        EqExp eqExp = new EqExp(parseRelExp());
        while (getType(now) == Type.EQL ||getType(now) == Type.NEQ) {
            eqExp.addOperator(now);
            peek();
            eqExp.addT(parseRelExp());
        }
        return eqExp;
    }

    /*
    LAndExp -> EqExp | LAndExp '&&' EqExp
    -------------------------------------
    LAndExp -> EqExp { '&&' EqExp }
     */
    public LAndExp parseLAndExp() {
        LAndExp lAndExp = new LAndExp(parseEqExp());
        while (getType(now) == Type.AND) {
            lAndExp.addOperator(now);
            peek();
            lAndExp.addT(parseEqExp());
        }
        return lAndExp;
    }

    /*
    LOrExp -> LAndExp | LOrExp '||' LAndExp
    ----------------------------------------
    LOrExp -> LAndExp { '||' LAndExp }
     */
    public LOrExp parseLOrExp() {
        LOrExp lOrExp = new LOrExp(parseLAndExp());
        while (getType(now) == Type.OR) {
            lOrExp.addOperator(now);
            peek();
            lOrExp.addT(parseLAndExp());
        }
        return lOrExp;
    }

    /*
    ConstExp -> AddExp
    -------------------
    Ident 为常量
     */
    public Exp parseConstExp() {
        return new ConstExp(parseAddExp());
    }
}
