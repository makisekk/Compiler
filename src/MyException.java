import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MyException extends Exception{
    private static String errors = "";
    private String code;
    private int line;
    
    public MyException(String code, int line) {
        this.code = code;
        this.line = line;
        errors = errors.concat(line + " " + code + "\n");
    }
    
    public static void printErrors() throws FileNotFoundException {
        File f = new File("./error.txt");
        f.delete();
        if (!errors.equals("")) {
            f = new File("./llvm_ir.txt");
            f.delete();
            PrintStream c = System.out;
            PrintStream o = new PrintStream("./llvm_ir.txt");
            System.setOut(o);
            System.out.print("define i32 @main() {\nret i32 0\n}\n");
            o.close();
            System.setOut(c);
        
            PrintStream console = System.out;
            PrintStream ps = new PrintStream("./error.txt");
            System.setOut(ps);
            System.out.print(errors);
            ps.close();
            System.setOut(console);
        }
    }
 
    
}
