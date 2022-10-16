package Error;

public class Error {
    private final Type type;
    private final int line;

    public Error(Type type, int line) {
        this.type = type;
        this.line = line;
    }

    @Override
    public String toString() {
        return line + " " + type;
    }
}
