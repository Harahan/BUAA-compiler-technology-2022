import Backend.MipsGenerator;
import Error.ErrorTable;
import Lexer.Lexer;
import Middle.MidCodeList;
import Middle.Optimization.DataFlow;
import Middle.Visitor;
import Syntax.Parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import static Backend.MipsGenerator.optimize;

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
            return;
        }
        DataFlow dataFlow = new DataFlow(MidCodeList.codes);
        write(MidCodeList.printMidCode(), "ir.txt");
        dataFlow.divideBlock();
        dataFlow.arriveDataAnalysis();
        dataFlow.activeDataAnalysis();
        if (optimize.get("BroadcastCode")) dataFlow.broadcastCode();
        if (optimize.get("DeleteDeadCode")) dataFlow.deleteDeadCode();
        if (optimize.get("ExtractLoopConstExp")) dataFlow.extractLoopConstExp();
        MidCodeList.codes = dataFlow.getCodes();
        write(MidCodeList.printMidCode(), "ir_opt.txt");
        write(new MipsGenerator(dataFlow.getCodes()).printMipsCode(true), "mips.txt");
    }
}
