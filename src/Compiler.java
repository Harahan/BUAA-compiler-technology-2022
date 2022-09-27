import Lexer.Lexer;
import Syntax.Parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Compiler {
    private static String read(String path) {
        File file = new File(path);
        byte[] bytes = new byte[(int) file.length()];
        try {
            new FileInputStream(file).read(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(bytes);
    }

    private static void write(String s, String path) {
        File file = new File(path);
        try {
            new FileOutputStream(file).write(s.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String source = read("testfile.txt");
        Lexer lexer = new Lexer(source);
        Parser parser = new Parser(lexer.getTokens());
        write(parser.toString(), "output.txt");
    }
}
