import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class Compiler {
    private static ArrayList<Token> tokens = new ArrayList<>();
    private static GrammarTreeNode root = new GrammarTreeNode(null);
    
    public static void main(String[] args) throws Exception{
        GetTokens.run(tokens);
        Parser.run(root, tokens);
        LlvmGenerator.run(root);
        MyException.printErrors();
        // printGrammarTree();
        
    }
    
    private static void printGrammarTree() {
        System.out.println("roottype " + root.getToken().getType().toString());
        Queue<GrammarTreeNode> queue = new LinkedList<GrammarTreeNode>();
        GrammarTreeNode p = root, s;
        queue.offer(root);
        s = p.getChild();
        while (s != null) {
            queue.offer(s);
            s = s.getNext();
        }
        p = root.getChild();
        while (p != null) {
            s = p.getChild();
            while (s != null) {
                queue.offer(s);
                s = s.getNext();
            }
            if (p.getNext() == null) {
                p = p.getParent().getChild().getChild();
            }
            else p = p.getNext();
        }
        System.out.println(queue.size());
        while ((p = queue.poll()) != null) {
            String str = "parent: " + p.getToken().getType().toString() + " sons: ";
            s = p.getChild();
            while (s != null) {
                str += s.getToken().getType().toString() + " ";
                s = s.getNext();
            }
            System.out.println(str);
        }
    }
    
}
