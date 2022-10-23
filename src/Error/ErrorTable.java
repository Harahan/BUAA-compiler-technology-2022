package Error;
import java.util.ArrayList;
import java.util.Collections;

public class ErrorTable {
    private static final ArrayList<Error> errors = new ArrayList<>();

    public static void add(Error e) {
        errors.add(e);
    }

    public static boolean isEmpty() {
        return errors.isEmpty();
    }

    public static String printError() {
        StringBuilder sb = new StringBuilder();
        Collections.sort(errors);
        errors.forEach(a -> sb.append(a).append("\n"));
        return sb.substring(0, sb.length() - 1);
    }
}
