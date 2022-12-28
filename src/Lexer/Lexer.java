package Lexer;

import java.util.ArrayList;
import java.util.HashMap;
import Error.ErrorTable;
import Error.Error;

public class Lexer {
    private final String input;
    private int pos = 0;
    private char ch;
    private int curLine = 1;
    private final ArrayList<Token> tokens = new ArrayList<>();
    private final HashMap<String, Type> str2type = new HashMap<String, Type>() {
        {
            // (Ident, IDENFR), (IntConst INTCON), (FormatString, STRCON)
            put("main", Type.MAINTK);           put("const", Type.CONSTTK);
            put("int", Type.INTTK);             put("break", Type.BREAKTK);
            put("continue", Type.CONTINUETK);   put("if", Type.IFTK);
            put("else", Type.ELSETK);           put("!", Type.NOT);
            put("&&", Type.AND);                put("||", Type.OR);
            put("while", Type.WHILETK);         put("getint", Type.GETINTTK);
            put("printf", Type.PRINTFTK);       put("return", Type.RETURNTK);
            put("+", Type.PLUS);                put("-", Type.MINU);
            put("void", Type.VOIDTK);           put("*", Type.MULT);
            put("/", Type.DIV);                 put("%", Type.MOD);
            put("<", Type.LSS);                 put("<=", Type.LEQ);
            put(">", Type.GRE);                 put(">=", Type.GEQ);
            put("==", Type.EQL);                put("!=", Type.NEQ);
            put("=", Type.ASSIGN);              put(";", Type.SEMICN);
            put(",", Type.COMMA);               put("(", Type.LPARENT);
            put(")", Type.RPARENT);             put("[", Type.LBRACK);
            put("]", Type.RBRACK);              put("{", Type.LBRACE);
            put("}", Type.RBRACE);              put("bitand", Type.BITAND);
        }
    };

    public Lexer(String input) {
        // this.input = input;
        input = input.replaceFirst("int tRue\\(\\)", "int true\\(\\)");
        input = input.replaceAll("tRue\\(\\)", "1");
        input = input.replaceFirst("int true\\(\\)", "int tRue\\(\\)");
        this.input = input;
        ch = getchar();
        while (ch != '\0') next();
        for (int i = 0; i < tokens.size(); ++i) {
            if (i + 3 < tokens.size() && tokens.get(i).getType() == Type.INTTK && tokens.get(i + 1).getType() == Type.IDENFR && tokens.get(i + 2).getType() == Type.ASSIGN && tokens.get(i + 3).getType() == Type.GETINTTK) {
                tokens.add(i + 2, new Token(Type.SEMICN, curLine, ";"));
                tokens.add(i + 3, tokens.get(i + 1));
                if (i + 8 < tokens.size() && tokens.get(i + 8).getType() == Type.COMMA) {
                    tokens.set(i + 8, new Token(Type.SEMICN, curLine, ";"));
                    tokens.add(i + 9, new Token(Type.INTTK, curLine, "int"));
                }
                i += 3;
            }
        }
        // System.out.println(tokens);
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    private char getchar() {
        char c = '\0';
        if (pos < input.length()) c = input.charAt(pos++);
        if (c == '\n') ++curLine;
        return c;
    }

    private void addNumber() {
        int num = 0, line = curLine;
        while (Character.isDigit(ch)) {
            num = (num << 3) + (num << 1) + ch - '0';
            ch = getchar();
        }
        tokens.add(new Token(Type.INTCON, line, num));
    }

    private void addWord() {
        StringBuilder sb = new StringBuilder();
        int line = curLine;
        while (Character.isLetter(ch) || ch == '_' || Character.isDigit(ch)) {
            sb.append(ch);
            ch = getchar();
        }
        String s = sb.toString();
        Type type = str2type.get(s);
        tokens.add(new Token(type != null ? type : Type.IDENFR, line, s));
    }

    // Error: a
    private void addFormatString() {
        // right quotation exist
        int line = curLine;
        ch = getchar();
        StringBuilder sb = new StringBuilder("\"");
        while (ch != '"') {
            sb.append(ch);
            ch = getchar();
        }
        ch = getchar();
        sb.append("\"");
        String s = sb.toString();
        for (int i = 1; i < s.length() - 1; ++i) {
            int c = s.charAt(i);
            boolean x = (c == 32 || c == 33 || (40 <= c && c <= 126 && c != 92));
            boolean y = (c == 92 && i + 1 < s.length() - 1 && s.charAt(i + 1) == 'n');
            boolean z = (s.charAt(i) == '%' && i + 1 < s.length() - 1 && s.charAt(i + 1) == 'd');
            if (!x && !y && !z) {
                ErrorTable.add(new Error(Error.Type.ILLEGAL_CHAR, line));
                break;
            }
        }
        tokens.add(new Token(Type.STRCON, line, s));
    }

    private void eatOffNotation() {
        if (ch == '/') {
            while (ch != '\n' && ch != '\0') ch = getchar(); //file end without '\n'
        } else {
            char p = '\0';
            ch = getchar();
            while (!(p == '*' && ch == '/')) {
                p = ch;
                ch = getchar();
            }
        }
        ch = getchar();
    }

    private void next() {
        int line = curLine;
        StringBuilder sb = new StringBuilder();

        if (ch == '\0') return;
        if (ch == '\n' || ch == '\t' || ch == ' ' || ch == '\r') {
            ch = getchar();
            return;
        }

        if (Character.isDigit(ch)) {
            addNumber();
        } else if (Character.isLetter(ch) || ch == '_') {
            addWord();
        } else if (ch == '"') {
            addFormatString();
        } else if (")([]{},;%*-+".indexOf(ch) != -1) {
            tokens.add(new Token(str2type.get(String.valueOf(ch)), line, String.valueOf(ch)));
            ch = getchar();
        } else if ("<>!=".indexOf(ch) != -1) {
            sb.append(ch);
            ch = getchar();
            if (ch == '=') {
                sb.append(ch);
                ch = getchar();
            }
            String s = sb.toString();
            tokens.add(new Token(str2type.get(s), line, s));
        } else if (ch == '|') {
            ch = getchar();
            if (ch == '|') tokens.add(new Token(str2type.get("||"), line, "||"));
            else tokens.add(new Token(Type.UNDEFINE, line));
            ch = getchar();
        } else if (ch == '&') {
            ch = getchar();
            if (ch == '&') tokens.add(new Token(str2type.get("&&"), line, "&&"));
            else tokens.add(new Token(Type.UNDEFINE, line));
            ch = getchar();
        } else if (ch == '/') {
            ch = getchar();
            if ("/*".indexOf(ch) != -1) eatOffNotation();
            else tokens.add(new Token(str2type.get("/"), line, "/"));
        } else {
            tokens.add(new Token(Type.UNDEFINE, line));
            ch = getchar();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) sb.append(token).append("\n");
        return sb.substring(0, sb.length() - 1);
    }
}
