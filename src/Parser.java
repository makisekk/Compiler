import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

public class Parser {
    private static Token cur;
    private static ArrayList<Token> tokens;
    private static int i = 0;
    private static boolean output = false;
    
    private static void getToken(GrammarTreeNode parent) {
        cur = tokens.get(i);
        GrammarTreeNode child = new GrammarTreeNode(cur);
        parent.addChild(child);
        child.setParent(parent);
        System.out.println(cur.getType() + " " + cur.getVal());
        i++;
    }
    
    private static Token peekNextToken() {
        return tokens.get(i);
    }
    
    private static Token peekNext2Token() {
        return tokens.get(i+1);
    }
    
    private static Token peekNext3Token() {
        return tokens.get(i+2);
    }
    
    public static void run(GrammarTreeNode root, ArrayList<Token> words) throws Exception{
        tokens = words;
        PrintStream console = System.out;
        PrintStream ps = new PrintStream("./output.txt");
        System.setOut(ps);
        CompUnit(root);
        ps.close();
        System.setOut(console);
        if (!output) {
            File output = new File("./output.txt");
            output.delete();
        }
    }
    
    private static boolean CompUnit(GrammarTreeNode root) throws MyException{
        root.setToken(new Token(TokenType.CompUnit, null, null));
        Token t = peekNextToken();
        while (t.getType() == TokenType.CONSTTK || (t.getType() == TokenType.INTTK && !(peekNext3Token().getType() == TokenType.LPARENT))) {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Decl, null, null));
            if(Decl(child)) {
                child.setParent(root);
                root.addChild(child);
            }
            t = peekNextToken();
        }
        while (t.getType() == TokenType.INTTK && peekNext2Token().getType() == TokenType.IDENFR || t.getType() == TokenType.VOIDTK) {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.FuncDef, null, null));
            if(FuncDef(child)) {
                child.setParent(root);
                root.addChild(child);
            }
            t = peekNextToken();
        }
        if (t.getType() == TokenType.INTTK && peekNext2Token().getType() == TokenType.MAINTK) {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.MainFuncDef, null, null));
            if(MainFuncDef(child)) {
                child.setParent(root);
                root.addChild(child);
            }
            System.out.println("<" + root.getToken().getType() + ">");
            return true;
        }
        return false;
    }
    
    private static boolean Decl(GrammarTreeNode root) throws MyException{
        Token t = peekNextToken();
        if (t.getType() == TokenType.CONSTTK) {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.ConstDecl, null, null));
            if(ConstDecl(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        else if (t.getType() == TokenType.INTTK) {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.VarDecl, null, null));
            if(VarDecl(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        else return false;
        return true;
    }
    
    private static boolean ConstDecl(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.CONSTTK) getToken(root);
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.BType, null, null));
        if(BType(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        child = new GrammarTreeNode(new Token(TokenType.ConstDef, null, null));
        if(ConstDef(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        while (peekNextToken().getType() == TokenType.COMMA) {
            getToken(root);
            child = new GrammarTreeNode(new Token(TokenType.ConstDef, null, null));
            if(ConstDef(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        if (peekNextToken().getType() == TokenType.SEMICN) getToken(root);
        else {                      // 漏符号处理漏写少写缺符号
            new MyException("i", cur.getLn());
            Token t = new Token(TokenType.SEMICN, ";", cur.getLn());
            tokens.add(i,t);
            getToken(root);
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean BType(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.INTTK) {
            getToken(root);
            return true;
        } else return false;
    }
    
    private static boolean ConstDef(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.IDENFR) getToken(root);
        if (peekNextToken().getType() == TokenType.LBRACK) {
            getToken(root);
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.ConstExp, null, null));
            if(ConstExp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
            if (peekNextToken().getType() == TokenType.RBRACK) {
                getToken(root);
            }
            else {
                new MyException("k", cur.getLn());
                Token t = new Token(TokenType.RBRACK, "]", cur.getLn());
                tokens.add(i,t);
                getToken(root);
            }
            if (peekNextToken().getType() == TokenType.LBRACK) {
                getToken(root);
                child = new GrammarTreeNode(new Token(TokenType.ConstExp, null, null));
                if(ConstExp(child)) {
                    child.setParent(root);
                    root.addChild(child);
                }
                if (peekNextToken().getType() == TokenType.RBRACK) {
                    getToken(root);
                }
                else {
                    new MyException("k", cur.getLn());
                    Token t = new Token(TokenType.RBRACK, "]", cur.getLn());
                    tokens.add(i,t);
                    getToken(root);
                }
            }
        }
        if (peekNextToken().getType() == TokenType.ASSIGN) getToken(root);
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.ConstInitVal, null, null));
        if(ConstInitVal(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean ConstInitVal(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.LBRACE) {
            getToken(root);
            if (peekNextToken().getType() == TokenType.RBRACE) {
                getToken(root);
            }
            else {
                GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.ConstInitVal, null, null));
                if (ConstInitVal(child)) {
                    child.setParent(root);
                    root.addChild(child);
                }
                while (peekNextToken().getType() == TokenType.COMMA) {
                    getToken(root);
                    child = new GrammarTreeNode(new Token(TokenType.ConstInitVal, null, null));
                    if (ConstInitVal(child)) {
                        child.setParent(root);
                        root.addChild(child);
                    }
                }
                if (peekNextToken().getType() == TokenType.RBRACE) getToken(root);
            }
        }
        else {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.ConstExp, null, null));
            if (ConstExp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean VarDecl(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.BType, null, null));
        if(BType(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        child = new GrammarTreeNode(new Token(TokenType.VarDef, null, null));
        if(VarDef(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        while (peekNextToken().getType() == TokenType.COMMA) {
            getToken(root);
            child = new GrammarTreeNode(new Token(TokenType.VarDef, null, null));
            if(VarDef(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        if (peekNextToken().getType() == TokenType.SEMICN) getToken(root);
        else {                      // 漏符号处理漏写少写缺符号
            new MyException("i", cur.getLn());
            Token t = new Token(TokenType.SEMICN, ";", cur.getLn());
            tokens.add(i,t);
            getToken(root);
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean VarDef(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.IDENFR) getToken(root);
        if (peekNextToken().getType() == TokenType.LBRACK) {
            getToken(root);
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.ConstExp, null, null));
            if(ConstExp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
            if (peekNextToken().getType() == TokenType.RBRACK) {
                getToken(root);
            }
            else {
                new MyException("k", cur.getLn());
                Token t = new Token(TokenType.RBRACK, "]", cur.getLn());
                tokens.add(i,t);
                getToken(root);
            }
            if (peekNextToken().getType() == TokenType.LBRACK) {
                getToken(root);
                child = new GrammarTreeNode(new Token(TokenType.ConstExp, null, null));
                if(ConstExp(child)) {
                    child.setParent(root);
                    root.addChild(child);
                }
                if (peekNextToken().getType() == TokenType.RBRACK) {
                    getToken(root);
                }
                else {
                    new MyException("k", cur.getLn());
                    Token t = new Token(TokenType.RBRACK, "]", cur.getLn());
                    tokens.add(i,t);
                    getToken(root);
                }
            }
        }
        if (peekNextToken().getType() == TokenType.ASSIGN) {
            getToken(root);
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.InitVal, null, null));
            if (InitVal(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean InitVal(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.LBRACE) {
            getToken(root);
            if (peekNextToken().getType() == TokenType.RBRACE) {
                getToken(root);
            }
            else {
                GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.InitVal, null, null));
                if (InitVal(child)) {
                    child.setParent(root);
                    root.addChild(child);
                }
                while (peekNextToken().getType() == TokenType.COMMA) {
                    getToken(root);
                    child = new GrammarTreeNode(new Token(TokenType.InitVal, null, null));
                    if (InitVal(child)) {
                        child.setParent(root);
                        root.addChild(child);
                    }
                }
                if (peekNextToken().getType() == TokenType.RBRACE) getToken(root);
            }
        }
        else {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Exp, null, null));
            if (Exp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean FuncDef(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.FuncType, null, null));
        if(FuncType(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        if (peekNextToken().getType() == TokenType.IDENFR) getToken(root);
        if (peekNextToken().getType() == TokenType.LPARENT) getToken(root);
        child = new GrammarTreeNode(new Token(TokenType.FuncFParams, null, null));
        if(FuncFParams(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        if (peekNextToken().getType() == TokenType.RPARENT) getToken(root);
        else {                      // 漏符号处理漏写少写缺符号
            new MyException("j", cur.getLn());
            Token t = new Token(TokenType.RPARENT, ")", cur.getLn());
            tokens.add(i,t);
            getToken(root);
        }
        child = new GrammarTreeNode(new Token(TokenType.Block, null, null));
        if(Block(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean MainFuncDef(GrammarTreeNode root) throws MyException {
        if (peekNextToken().getType() == TokenType.INTTK) getToken(root);
        if (peekNextToken().getType() == TokenType.MAINTK) getToken(root);
        if (peekNextToken().getType() == TokenType.LPARENT) getToken(root);
        if (peekNextToken().getType() == TokenType.RPARENT) getToken(root);
        else {                      // 漏符号处理漏写少写缺符号
            new MyException("j", cur.getLn());
            Token t = new Token(TokenType.RPARENT, ")", cur.getLn());
            tokens.add(i,t);
            getToken(root);
        }
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Block, null, null));
        if(Block(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean FuncType(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.VOIDTK) getToken(root);
        else if (peekNextToken().getType() == TokenType.INTTK) getToken(root);
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean FuncFParams(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.FuncFParam, null, null));
        if(FuncFParam(child)) {
            child.setParent(root);
            root.addChild(child);
        } else return false;
        while (peekNextToken().getType() == TokenType.COMMA) {
            getToken(root);
            child = new GrammarTreeNode(new Token(TokenType.FuncFParam, null, null));
            if(FuncFParam(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean FuncFParam(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.BType, null, null));
        if(BType(child)) {
            child.setParent(root);
            root.addChild(child);
        } else return false;
        if (peekNextToken().getType() == TokenType.IDENFR) getToken(root);
        if (peekNextToken().getType() == TokenType.LBRACK) {
            getToken(root);
            if (peekNextToken().getType() == TokenType.RBRACK) getToken(root);
            else {
                new MyException("k", cur.getLn());
                Token t = new Token(TokenType.RBRACK, "]", cur.getLn());
                tokens.add(i,t);
                getToken(root);
            }
            while (peekNextToken().getType() == TokenType.LBRACK) {
                getToken(root);
                child = new GrammarTreeNode(new Token(TokenType.ConstExp, null, null));
                if(ConstExp(child)) {
                    child.setParent(root);
                    root.addChild(child);
                }
                if (peekNextToken().getType() == TokenType.RBRACK) getToken(root);
                else {                      // 漏符号处理漏写少写缺符号
                    new MyException("k", cur.getLn());
                    Token t = new Token(TokenType.RBRACK, "]", cur.getLn());
                    tokens.add(i,t);
                    getToken(root);
                }
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean Block(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.LBRACE) getToken(root);
        while (peekNextToken().getType() != TokenType.RBRACE) {
            int ln = peekNextToken().getLn();
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.BlockItem, null, null));
            if(BlockItem(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        if (peekNextToken().getType() == TokenType.RBRACE) getToken(root);
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean BlockItem(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.CONSTTK || peekNextToken().getType() == TokenType.INTTK ) {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Decl, null, null));
            if(Decl(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        } else {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Stmt, null, null));
            if(Stmt(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        return true;
    }
    
    private static boolean Stmt(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.LBRACE) {    // 语句块
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Block, null, null));
            if(Block(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        else if (peekNextToken().getType() == TokenType.IFTK) {    //条件句
            getToken(root);
            if (peekNextToken().getType() == TokenType.LPARENT) getToken(root);
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Cond, null, null));
            if (Cond(child)) {
                child.setParent(root);
                root.addChild(child);
            }
            if (peekNextToken().getType() == TokenType.RPARENT) getToken(root);
            else {                      // 漏符号处理漏写少写缺符号
                new MyException("j", cur.getLn());
                Token t = new Token(TokenType.RPARENT, ")", cur.getLn());
                tokens.add(i,t);
                getToken(root);
            }
            child = new GrammarTreeNode(new Token(TokenType.Stmt, null, null));
            if (Stmt(child)) {
                child.setParent(root);
                root.addChild(child);
            }
            if (peekNextToken().getType() == TokenType.ELSETK) {
                getToken(root);
                child = new GrammarTreeNode(new Token(TokenType.Stmt, null, null));
                if (Stmt(child)) {
                    child.setParent(root);
                    root.addChild(child);
                }
            }
        }
        else if (peekNextToken().getType() == TokenType.WHILETK) {    // 循环
            getToken(root);
            if (peekNextToken().getType() == TokenType.LPARENT) getToken(root);
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Cond, null, null));
            if (Cond(child)) {
                child.setParent(root);
                root.addChild(child);
            }
            if (peekNextToken().getType() == TokenType.RPARENT) getToken(root);
            else {                      // 漏符号处理漏写少写缺符号
                new MyException("j", cur.getLn());
                Token t = new Token(TokenType.RPARENT, ")", cur.getLn());
                tokens.add(i,t);
                getToken(root);
            }
            child = new GrammarTreeNode(new Token(TokenType.Stmt, null, null));
            if (Stmt(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        else if (peekNextToken().getType() == TokenType.BREAKTK || peekNextToken().getType() == TokenType.CONTINUETK) {     // 跳转
            getToken(root);
            if (peekNextToken().getType() == TokenType.SEMICN) getToken(root);
            else {
                new MyException("i", cur.getLn());
                Token t = new Token(TokenType.SEMICN, ";", cur.getLn());
                tokens.add(i,t);
                getToken(root);
            }
        }
        else if (peekNextToken().getType() == TokenType.RETURNTK) {     // return
            getToken(root);
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Exp, null, null));
            if(Exp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
            if (peekNextToken().getType() == TokenType.SEMICN) getToken(root);
            else {
                new MyException("i", cur.getLn());
                Token t = new Token(TokenType.SEMICN, ";", cur.getLn());
                tokens.add(i,t);
                getToken(root);
            }
        }
        else if (peekNextToken().getType() == TokenType.PRINTFTK) {     // 打印
            getToken(root);
            if (peekNextToken().getType() == TokenType.LPARENT) getToken(root);
            if (peekNextToken().getType() == TokenType.STRCON) getToken(root);
            else {
                new MyException("a", root.getToken().getLn());
                int curLn = root.getToken().getLn();
                while (tokens.get(i).getLn() != curLn + 1) {
                    ++i;
                }
                GrammarTreeNode fill = new GrammarTreeNode(new Token(TokenType.STRCON, "\"error\"", curLn));
                root.addChild(fill);
                fill.setParent(root);
                System.out.println(fill.getType() + " " + fill.getVal());
                fill = new GrammarTreeNode(new Token(TokenType.RPARENT, ")", curLn));
                root.addChild(fill);
                fill.setParent(root);
                System.out.println(fill.getType() + " " + fill.getVal());
                fill = new GrammarTreeNode(new Token(TokenType.SEMICN, ";", curLn));
                root.addChild(fill);
                fill.setParent(root);
                System.out.println(fill.getType() + " " + fill.getVal());
                return true;
            }
            while (peekNextToken().getType() == TokenType.COMMA) {
                getToken(root);
                GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Exp, null, null));
                if(Exp(child)) {
                    child.setParent(root);
                    root.addChild(child);
                }
            }
            if (peekNextToken().getType() == TokenType.RPARENT) getToken(root);
            else {                      // 漏符号处理漏写少写缺符号
                new MyException("j", cur.getLn());
                Token t = new Token(TokenType.RPARENT, ")", cur.getLn());
                tokens.add(i,t);
                getToken(root);
            }
            if (peekNextToken().getType() == TokenType.SEMICN) getToken(root);
            else {
                new MyException("i", cur.getLn());
                Token t = new Token(TokenType.SEMICN, ";", cur.getLn());
                tokens.add(i,t);
                getToken(root);
            }
        }
        else {  // 表达式语句或赋值语句
            int ii = i;
            int curLn = peekNextToken().getLn();
            while (ii < tokens.size() && tokens.get(ii).getType() != TokenType.SEMICN && tokens.get(ii).getType() != TokenType.ASSIGN) {
                if (tokens.get(ii).getLn() > curLn) {   // 缺失分号
                    new MyException("i", curLn);
                    Token t = new Token(TokenType.SEMICN, ";", curLn);
                    tokens.add(ii, t);
                    break;
                }
                ii++;
            }
            if (ii == tokens.size() || tokens.get(ii).getType() == TokenType.SEMICN) { // 表达式语句
                GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Exp, null, null));
                if (Exp(child)) {
                    child.setParent(root);
                    root.addChild(child);
                }
                if (peekNextToken().getType() == TokenType.SEMICN) getToken(root);
                else {
                    new MyException("i", cur.getLn());
                    Token t = new Token(TokenType.SEMICN, ";", cur.getLn());
                    tokens.add(i,t);
                    getToken(root);
                }
            }
            else if (tokens.get(ii).getType() == TokenType.ASSIGN) { // 赋值语句
                GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.LVal, null, null));
                if(LVal(child)) {
                    child.setParent(root);
                    root.addChild(child);
                }
                if (peekNextToken().getType() == TokenType.ASSIGN) getToken(root);
                if (peekNextToken().getType() == TokenType.GETINTTK) {
                    getToken(root);
                    if (peekNextToken().getType() == TokenType.LPARENT) getToken(root);
                    if (peekNextToken().getType() == TokenType.RPARENT) getToken(root);
                    else {                      // 漏符号处理漏写少写缺符号
                        new MyException("j", cur.getLn());
                        Token t = new Token(TokenType.RPARENT, ")", cur.getLn());
                        tokens.add(i,t);
                        getToken(root);
                    }
                    if (peekNextToken().getType() == TokenType.SEMICN) getToken(root);
                    else {
                        new MyException("i", cur.getLn());
                        Token t = new Token(TokenType.SEMICN, ";", cur.getLn());
                        tokens.add(i,t);
                        getToken(root);
                    }
                } else {
                    child = new GrammarTreeNode(new Token(TokenType.Exp, null, null));
                    if(Exp(child)) {
                        child.setParent(root);
                        root.addChild(child);
                    }
                    if (peekNextToken().getType() == TokenType.SEMICN) getToken(root);
                    else {
                        new MyException("i", cur.getLn());
                        Token t = new Token(TokenType.SEMICN, ";", cur.getLn());
                        tokens.add(i,t);
                        getToken(root);
                    }
                }
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean Exp(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.AddExp, null, null));
        if(AddExp(child)) {
            child.setParent(root);
            root.addChild(child);
        } else return false;
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean Cond(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.LOrExp, null, null));
        if(LOrExp(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean LVal(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.IDENFR) getToken(root);
        while (peekNextToken().getType() == TokenType.LBRACK) {
            getToken(root);
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Exp, null, null));
            if (Exp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
            if (peekNextToken().getType() == TokenType.RBRACK) {
                getToken(root);
            }
            else {
                new MyException("k", cur.getLn());
                Token t = new Token(TokenType.RBRACK, "]", cur.getLn());
                tokens.add(i,t);
                getToken(root);
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean PrimaryExp(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.IDENFR) {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.LVal, null, null));
            if(LVal(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        else if (peekNextToken().getType() == TokenType.INTCON) {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Number, null, null));
            if (Number(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        else if (peekNextToken().getType() == TokenType.LPARENT) {
                getToken(root);
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Exp, null, null));
            if (Exp(child)) {
                child.setParent(root);
                root.addChild(child);
            } else return false;
            if (peekNextToken().getType() == TokenType.RPARENT) getToken(root);
            else return false;
        }
        else return false;
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean Number(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.INTCON) getToken(root);
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean UnaryExp(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.IDENFR && peekNext2Token().getType() == TokenType.LPARENT) {
            getToken(root);
            getToken(root);
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.FuncRParams, null, null));
            if (FuncRParams(child)) {
                child.setParent(root);
                root.addChild(child);
            }
            if (peekNextToken().getType() == TokenType.RPARENT) getToken(root);
            else {                      // 漏符号处理漏写少写缺符号
                new MyException("j", cur.getLn());
                Token t = new Token(TokenType.RPARENT, ")", cur.getLn());
                tokens.add(i,t);
                getToken(root);
            }
        } else if (peekNextToken().getType() == TokenType.IDENFR || peekNextToken().getType() == TokenType.LPARENT || peekNextToken().getType() == TokenType.INTCON){
                GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.PrimaryExp, null, null));
                if(PrimaryExp(child)) {
                    child.setParent(root);
                    root.addChild(child);
                } else return false;
        } else if (peekNextToken().getType() == TokenType.PLUS || peekNextToken().getType() == TokenType.MINU || peekNextToken().getType() == TokenType.NOT) {
            GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.UnaryOp, null, null));
            if(UnaryOp(child)) {
                child.setParent(root);
                root.addChild(child);
            } else return false;
            child = new GrammarTreeNode(new Token(TokenType.UnaryExp, null, null));
            if(UnaryExp(child)) {
                child.setParent(root);
                root.addChild(child);
            } else return false;
        } else return false;
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean UnaryOp(GrammarTreeNode root) throws MyException{
        if (peekNextToken().getType() == TokenType.PLUS || peekNextToken().getType() == TokenType.MINU || peekNextToken().getType() == TokenType.NOT) {
            getToken(root);
            System.out.println("<" + root.getToken().getType() + ">");
        }
        else return false;
        return true;
    }
    
    private static boolean FuncRParams(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.Exp, null, null));
        if(Exp(child)) {
            child.setParent(root);
            root.addChild(child);
        } else return false;
        while (peekNextToken().getType() == TokenType.COMMA) {
            getToken(root);
            child = new GrammarTreeNode(new Token(TokenType.Exp, null, null));
            if(Exp(child)) {
                child.setParent(root);
                root.addChild(child);
            } else return false;
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean MulExp(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.UnaryExp, null, null));
        if(UnaryExp(child)) {
            child.setParent(root);
            root.addChild(child);
        } else return false;
        while (peekNextToken().getType() == TokenType.MULT || peekNextToken().getType() == TokenType.DIV || peekNextToken().getType() == TokenType.MOD) {   // 左递归特殊处理，树节点升一级(只改输出，不改语法树)
            System.out.println("<" + root.getToken().getType() + ">");
            getToken(root);
            child = new GrammarTreeNode(new Token(TokenType.UnaryExp, null, null));
            if(UnaryExp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean AddExp(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.MulExp, null, null));
        if(MulExp(child)) {
            child.setParent(root);
            root.addChild(child);
        } else return false;
        while (peekNextToken().getType() == TokenType.PLUS || peekNextToken().getType() == TokenType.MINU) {   // 左递归特殊处理，树节点升一级(只改输出，不改语法树)
            System.out.println("<" + root.getToken().getType() + ">");
            getToken(root);
            child = new GrammarTreeNode(new Token(TokenType.MulExp, null, null));
            if(MulExp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean RelExp(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.AddExp, null, null));
        if(AddExp(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        while (peekNextToken().getType() == TokenType.LSS || peekNextToken().getType() == TokenType.LEQ || peekNextToken().getType() == TokenType.GEQ || peekNextToken().getType() == TokenType.GRE) {   // 左递归特殊处理，树节点升一级(只改输出，不改语法树)
            System.out.println("<" + root.getToken().getType() + ">");
            getToken(root);
            child = new GrammarTreeNode(new Token(TokenType.AddExp, null, null));
            if(AddExp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean EqExp(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.RelExp, null, null));
        if(RelExp(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        while (peekNextToken().getType() == TokenType.EQL || peekNextToken().getType() == TokenType.NEQ) {   // 左递归特殊处理，树节点升一级(只改输出，不改语法树)
            System.out.println("<" + root.getToken().getType() + ">");
            getToken(root);
            child = new GrammarTreeNode(new Token(TokenType.RelExp, null, null));
            if(RelExp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean LAndExp(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.EqExp, null, null));
        if(EqExp(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        while (peekNextToken().getType() == TokenType.AND) {   // 左递归特殊处理，树节点升一级(只改输出，不改语法树)
            System.out.println("<" + root.getToken().getType() + ">");
            getToken(root);
            child = new GrammarTreeNode(new Token(TokenType.EqExp, null, null));
            if(EqExp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean LOrExp(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.LAndExp, null, null));
        if(LAndExp(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        while (peekNextToken().getType() == TokenType.OR) {   // 左递归特殊处理，树节点升一级(只改输出，不改语法树)
            System.out.println("<" + root.getToken().getType() + ">");
            getToken(root);
            child = new GrammarTreeNode(new Token(TokenType.LAndExp, null, null));
            if(LAndExp(child)) {
                child.setParent(root);
                root.addChild(child);
            }
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
    private static boolean ConstExp(GrammarTreeNode root) throws MyException{
        GrammarTreeNode child = new GrammarTreeNode(new Token(TokenType.AddExp, null, null));
        if(AddExp(child)) {
            child.setParent(root);
            root.addChild(child);
        }
        System.out.println("<" + root.getToken().getType() + ">");
        return true;
    }
    
}
