package Error;

public class Error implements Comparable<Error> {
    private final Type type;
    private final int line;

    public Error(Type type, int line) {
        this.type = type;
        this.line = line;
    }

    public int getLine() {
        return line;
    }

    public Type getType() {
        return type;
    }

    public String getTypeLabel() {
        return type.getLabel();
    }

    @Override
    public String toString() {
        return line + " " + type;
    }


    @Override
    public int compareTo(Error o) {
        return Integer.compare(line, o.getLine());
    }

    public enum Type {
        ILLEGAL_CHAR("a"),              DUPLICATE_IDENT("b"),
        UNDEFINE_IDENT("c"),            MISMATCH_PARAM_NUM("d"),
        MISMATCH_PARAM_TYPE("e"),       SPARE_RETURN_VALUE("f"),
        LACK_RETURN_VALUE("g"),         CHANGE_CONST_VALUE("h"),
        LACK_SEMICOLON("i"),            LACK_RIGHT_PARENT("j"),
        LACK_RIGHT_BRACKET("k"),        MISMATCH_PRINT_FORMAT_CHAR("l"),
        BREAK_CONTINUE_OUT_LOOP("m"),
        ;
        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
