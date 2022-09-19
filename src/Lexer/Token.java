package Lexer;

public class Token {
    private final Type type;
    private final int line;
    private final int intVal;
    private final String strVal;

    public Token(Type type, int line) {
        this.type = type;
        this.line = line;
        this.intVal = -1;
        this.strVal = null;
    }

    public Token(Type type, int line, int intVal) {
        this.type = type;
        this.line = line;
        this.intVal = intVal;
        this.strVal = null;
    }

    public Token(Type type, int line, String strVal) {
        this.type = type;
        this.line = line;
        this.intVal = -1;
        this.strVal = strVal;
    }

    public Type getType() {
        return type;
    }

    public int getLine() {
        return line;
    }

    public int getIntVal() {
        return intVal;
    }

    public String getStrVal() {
        return strVal;
    }

    @Override
    public String toString() {
        if (getIntVal() != -1) return getType() + " " + getIntVal();
        if (getStrVal() != null) return getType() + " " + getStrVal();
        return getType().toString();
    }
}
