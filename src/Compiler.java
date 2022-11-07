import Backend.MipsGenerator;
import Lexer.Lexer;
import Middle.MidCodeList;
import Middle.Util.Code;
import Middle.Visitor;
import Syntax.Parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.regex.Matcher;

import Error.ErrorTable;

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
        new Visitor(parser.getCompUnit());
        if (ErrorTable.isEmpty()) {
            write(parser.toString(), "output.txt");
        } else {
            write(ErrorTable.printError(), "error.txt");
            // return;
        }
        write(MidCodeList.printMidCode(), "midCode.txt");
        write(new MipsGenerator().printMipsCode(), "mips.txt");
    }
}
