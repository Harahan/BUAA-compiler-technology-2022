package Error;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class ErrorTable {
    private static final HashSet<Error> errors = new HashSet<Error>();

    public static void add(Error e) {
        errors.add(e);
    }

    public static boolean isEmpty() {
        return errors.isEmpty();
    }

    public static String printError() {
        StringBuilder sb = new StringBuilder();
        ArrayList<Error> arr = new ArrayList<Error>(errors);
        Collections.sort(arr);
        arr.forEach(a -> sb.append(a).append("\n"));
        return sb.substring(0, sb.length() - 1);
    }
}
