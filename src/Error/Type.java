package Error;

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

    @Override
    public String toString() {
        return label;
    }
}
