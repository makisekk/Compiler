import java.io.PrintStream;
import java.util.*;

public class LlvmGenerator {
    
    private static final SymTable globalTable = new SymTable();
    private static final HashMap<String, FuncTable> funcTables = new HashMap<>();
    private static SymTable curTable = globalTable;
    private static int regId = 0;
    private static boolean global = true;
    private static boolean debug = false;
    private static int loopDepth = 0;
    private static boolean isVoid = false;
    private static boolean hasRet;  // 函数体中是否有返回语句
    private static int firstRetLine;
    private static HashMap<TokenType, String> condMap = new HashMap<>();
    private static ArrayList<Loop> loopStack = new ArrayList<>();

    public static void run(GrammarTreeNode root) throws Exception{
        PrintStream ps = new PrintStream("./llvm_ir.txt");
        PrintStream console = System.out;
        System.setOut(ps);
        condMap.put(TokenType.LSS, "slt");
        condMap.put(TokenType.GRE, "sgt");
        condMap.put(TokenType.LEQ, "sle");
        condMap.put(TokenType.GEQ, "sge");
        condMap.put(TokenType.EQL, "eq");
        condMap.put(TokenType.NEQ, "ne");
        if (root.getType() == TokenType.CompUnit) {
            CompUnit(root);
        }
        
        ps.close();
        System.setOut(console);
    }
    
    private static void CompUnit(GrammarTreeNode p) {   // parent node
        GrammarTreeNode c = p.getChild();
        if (c == null) return;
        System.out.print("declare i32 @getint()          ; 读取一个整数\n");
        System.out.print("declare void @putint(i32)      ; 输出一个整数\n");
        System.out.print("declare void @putch(i32)       ; 输出一个字符\n");
        System.out.print("declare void @putstr(i8*)      ; 输出字符串\n");
        //System.out.print("declare void @memset(i32*, i32, i32)\n");
        System.out.print("\n");
        while (c != null && c.getType() == TokenType.Decl) {
            Decl(c);
            c = c.getNext();
        }
        System.out.print("\n");
        global = false;
        if (c == null) return;
        while (c != null && c.getType() == TokenType.FuncDef) {
            FuncDef(c);
            System.out.print("\n");
            c = c.getNext();
        }
        if (c == null) return;
        if (c.getType() == TokenType.MainFuncDef) {
            MainFuncDef(c);
        }
    }
    
    private static void Decl(GrammarTreeNode p) {
        if (p.getChildType(0) == TokenType.ConstDecl) ConstDecl(p.getChild());
        else if (p.getChildType(0) == TokenType.VarDecl) VarDecl(p.getChild());
    }
    
    private static void ConstDecl(GrammarTreeNode p) {  // 只进符号表，不需要生成代码
        GrammarTreeNode c = p.getChild(2);
        while (c != null) {
            ConstDef(c);
            c = c.getNext().getNext();
        }
    }
    
    private static void ConstDef(GrammarTreeNode p) {
        if (p.getChildType(2) == TokenType.ConstInitVal) {  // 普通变量
            String id = p.getChild().getVal();
            String val = ConstInitVal(p.getChild(2)).get(0);
            Symbol s = new Symbol(id, val, SymClass.VAR, p.getChild().getToken().getLn(), true);
            if (curTable.findHere(id) == null) {
                curTable.putSym(s);
            } else { new MyException("b", s.getLn()); }
        }
          else if (p.getChildsSize() == 6) {  // 一维数组
            String id = p.getChild().getVal();
            int size = Integer.parseInt(ConstExp(p.getChild(2)).getVal());
            ArrayList<String> vals = ConstInitVal(p.getChild(5));
            if (global) {
                String str = "@" + id + " = " + "dso_local constant [" + size + " x i32] [";
                for (int i = 0; i < size; i++) {
                    str += "i32 " + vals.get(i) + ",";
                }
                str = str.substring(0, str.length() - 1) + "]";
                System.out.println(str);
                Symbol s = new Symbol(id, size, vals, p.getChild().getToken().getLn(), true, "@" + id);
                if (curTable.findHere(id) == null) {
                    curTable.putSym(s);
                } else {
                    new MyException("b", s.getLn());
                }
            } else {
                System.out.println("%v" + regId + " = alloca [" + size + " x i32]");
                regId++;
                Symbol s = new Symbol(id, size, vals, p.getChild().getToken().getLn(), true, "%v" + (regId-1));
                if (curTable.findHere(id) == null) {
                    curTable.putSym(s);
                } else {
                    new MyException("b", s.getLn());
                }
                System.out.println("%v" + regId + " = getelementptr [" + size + " x i32], [" + size + " x i32]* " + "%v" + (regId-1) + ", i32 0, i32 0");
                regId++;
                String head = "%v" + (regId-1);
                //System.out.println("call void @memset(i32* %v" + (regId-1) + ", i32 0, i32 " + size*4 + ")");
                for (int i=0; i<size; i++) {
                    System.out.println("%v" + regId + " = getelementptr i32, i32* " + head + ", i32 " + i);
                    regId++;
                    String val = vals.get(i);
                    System.out.println("store i32 " + val + ", i32* %v" + (regId-1));
                }
            }
        }
        else if (p.getChildsSize() == 9) {  // 二维数组
            String id = p.getChild().getVal();
            int size1 = Integer.parseInt(ConstExp(p.getChild(2)).getVal());
            int size2 = Integer.parseInt(ConstExp(p.getChild(5)).getVal());
            ArrayList<String> vals = ConstInitVal(p.getChild(8));
            if (global) {
                String str = "@" + id + " = " + "dso_local constant [" + size1 + " x [" + size2 + " x i32]] [";
                for (int i = 0; i < size1; i++) {
                    str += "[" + size2 + " x i32] [";
                    for (int j = 0; j < size2; j++) {
                        str += "i32 " + vals.get(i * size2 + j) + ",";
                    }
                    str = str.substring(0, str.length() - 1) + "],";
                }
                str = str.substring(0, str.length() - 1) + "]";
                System.out.println(str);
                Symbol s = new Symbol(id, size1, size2, vals, p.getChild().getToken().getLn(), true, "@" + id);
                if (curTable.findHere(id) == null) {
                    curTable.putSym(s);
                } else {
                    new MyException("b", s.getLn());
                }
            } else {
                System.out.println("%v" + regId++ + " = alloca [" + size1 + " x [" + size2 + " x i32]]");
                Symbol s = new Symbol(id, size1, size2, vals, p.getChild().getToken().getLn(), true, "%v" + (regId-1));
                if (curTable.findHere(id) == null) {
                    curTable.putSym(s);
                } else {
                    new MyException("b", s.getLn());
                }
                System.out.println("%v" + regId + " = getelementptr " + "[" + size1 + " x [" + size2 + " x i32]]" + ", " + "[" + size1 + " x [" + size2 + " x i32]]" + "* %v" + (regId-1) + ", i32 0, i32 0");
                regId++;
                System.out.println("%v" + regId + " = getelementptr [" + size2 + " x i32], [" + size2 + " x i32]* " + "%v" + (regId-1) + ", i32 0, i32 0");
                regId++;
                String head = "%v" + (regId-1);
                //System.out.println("call void @memset(i32* %v" + (regId-1) + ", i32 0, i32 " + size1*size2*4 + ")");
                for (int i=0; i<size1*size2; i++) {
                    System.out.println("%v" + regId + " = getelementptr i32, i32* " + head + ", i32 " + i);
                    regId++;
                    String val = vals.get(i);
                    System.out.println("store i32 " + val + ", i32* %v" + (regId-1));
                }
            }
        }
    }
    
    private static void VarDecl(GrammarTreeNode p) {
        GrammarTreeNode c = p.getChild(1);
        while (c != null) {
            VarDef(c);
            c = c.getNext().getNext();
        }
    }
    
    private static void VarDef(GrammarTreeNode p) {
        if (p.getChildsSize() == 1 || p.getChildType(1) == TokenType.ASSIGN) { // 普通变量定义
            if (p.getChildsSize() == 1) {   // 无初值
                String id = p.getChild().getVal();
                if (global) {
                    if (curTable.findHere(id) == null) {
                        System.out.print("@" + id + " = dso_local global i32 0\n");
                        Symbol s = new Symbol(id, "0", SymClass.VAR, p.getToken().getLn(), "@" + id);
                        curTable.putSym(s);
                    } else { new MyException("b", p.getChild().getToken().getLn()); }
                }
                else {
                    if (curTable.findHere(id) == null) {
                        System.out.print("%v" + regId + " = alloca i32\n");
                        Symbol s = new Symbol(id, SymClass.VAR, p.getToken().getLn(), "%v" + regId);
                        curTable.putSym(s);
                        regId++;
                    } else { new MyException("b", p.getChild().getToken().getLn()); }
                }
            }
            else {  // 有初值
                String id = p.getChild().getVal();
                String val = InitVal(p.getChild(2)).get(0);
                if (global) {
                    if (curTable.findHere(id) == null) {
                        System.out.print("@" + id + " = dso_local global i32 " + val + "\n");
                        Symbol s = new Symbol(id, val, SymClass.VAR, p.getToken().getLn(), "@" + id);
                        curTable.putSym(s);
                    } else { new MyException("b", p.getChild().getToken().getLn()); }
                }
                else {
                    if (curTable.findHere(id) == null) {
                        System.out.print("%v" + regId + " = alloca i32\n");
                        System.out.print("store i32 " + val + ", i32* %v" + regId + "\n");
                        Symbol s = new Symbol(id, val, SymClass.VAR, p.getToken().getLn(), "%v" + regId);
                        curTable.putSym(s);
                        regId++;
                    } else { new MyException("b", p.getChild().getToken().getLn()); }
                }
            }
        }
        else if (p.getChildsSize() == 4) { // 一维变量无初值
            String id = p.getChild().getVal();
            int size = Integer.parseInt(ConstExp(p.getChild(2)).getVal());
            if (global) {
                String str = "@" + id + " = " + "dso_local global [" + size + " x i32] zeroinitializer";
                System.out.println(str);
                if (curTable.findHere(id) == null) {
                    Symbol s = new Symbol(id, size, new ArrayList<>(), p.getToken().getLn(), "@" + id);
                    curTable.putSym(s);
                } else {
                    new MyException("b", p.getChild().getToken().getLn());
                }
            } else {
                System.out.println("%v" + regId + " = alloca [" + size + " x i32]");
                regId++;
                Symbol s = new Symbol(id, size, null, p.getChild().getToken().getLn(), "%v" + (regId-1));
                if (curTable.findHere(id) == null) {
                    curTable.putSym(s);
                } else {
                    new MyException("b", s.getLn());
                }
                System.out.println("%v" + regId + " = getelementptr [" + size + " x i32], [" + size + " x i32]* " + "%v" + (regId-1) + ", i32 0, i32 0");
                regId++;
                //System.out.println("call void @memset(i32* %v" + (regId-1) + ", i32 0, i32 " + size*4 + ")");
            }
        }
        else if (p.getChildsSize() == 6) { // 一维变量有初值
            String id = p.getChild().getVal();
            int size = Integer.parseInt(ConstExp(p.getChild(2)).getVal());
            ArrayList<String> vals = InitVal(p.getChild(5));
            if (global) {
                String str = "@" + id + " = " + "dso_local global [" + size + " x i32] [";
                for (int i = 0; i < size; i++) {
                    str += "i32 " + vals.get(i) + ",";
                }
                str = str.substring(0, str.length() - 1) + "]";
                System.out.println(str);
                Symbol s = new Symbol(id, size, vals, p.getChild().getToken().getLn(), "@" + id);
                if (curTable.findHere(id) == null) {
                    curTable.putSym(s);
                } else {
                    new MyException("b", s.getLn());
                }
            } else {
                System.out.println("%v" + regId + " = alloca [" + size + " x i32]");
                regId++;
                Symbol s = new Symbol(id, size, vals, p.getChild().getToken().getLn(),  "%v" + (regId-1));
                if (curTable.findHere(id) == null) {
                    curTable.putSym(s);
                } else {
                    new MyException("b", s.getLn());
                }
                System.out.println("%v" + regId + " = getelementptr [" + size + " x i32], [" + size + " x i32]* " + "%v" + (regId-1) + ", i32 0, i32 0");
                regId++;
                String head = "%v" + (regId-1);
                //System.out.println("call void @memset(i32* %v" + (regId-1) + ", i32 0, i32 " + size*4 + ")");
                for (int i=0; i<size; i++) {
                    System.out.println("%v" + regId + " = getelementptr i32, i32* " + head + ", i32 " + i);
                    regId++;
                    String val = vals.get(i);
                    System.out.println("store i32 " + val + ", i32* %v" + (regId-1));
                }
            }
        }
        else if (p.getChildsSize() == 7) { // 二维变量无初值
            String id = p.getChild().getVal();
            int size1 = Integer.parseInt(ConstExp(p.getChild(2)).getVal());
            int size2 = Integer.parseInt(ConstExp(p.getChild(5)).getVal());
            if (global) {
                String str = "@" + id + " = " + "dso_local global [" + size1 + " x [" + size2 + " x i32]] zeroinitializer";
                System.out.println(str);
                if (curTable.findHere(id) == null) {
                    Symbol s = new Symbol(id, size1, size2, new ArrayList<>(), p.getToken().getLn(), "@" + id);
                    curTable.putSym(s);
                } else {
                    new MyException("b", p.getChild().getToken().getLn());
                }
            } else {
                System.out.println("%v" + regId + " = alloca [" + size1 + " x [" + size2 + " x i32]]");
                regId++;
                Symbol s = new Symbol(id, size1, size2, null, p.getChild().getToken().getLn(), "%v" + (regId-1));
                if (curTable.findHere(id) == null) {
                    curTable.putSym(s);
                } else {
                    new MyException("b", s.getLn());
                }
                System.out.println("%v" + regId + " = getelementptr " + "[" + size1 + " x [" + size2 + " x i32]]" + ", " + "[" + size1 + " x [" + size2 + " x i32]]" + "* %v" + (regId-1) + ", i32 0, i32 0");
                regId++;
                System.out.println("%v" + regId + " = getelementptr [" + size2 + " x i32], [" + size2 + " x i32]* " + "%v" + (regId-1) + ", i32 0, i32 0");
                regId++;
                //System.out.println("call void @memset(i32* %v" + (regId-1) + ", i32 0, i32 " + size1*size2*4 + ")");
            }
        }
        else if (p.getChildsSize() == 9) { // 二维变量有初值
            String id = p.getChild().getVal();
            int size1 = Integer.parseInt(ConstExp(p.getChild(2)).getVal());
            int size2 = Integer.parseInt(ConstExp(p.getChild(5)).getVal());
            ArrayList<String> vals = InitVal(p.getChild(8));
            if (global) {
                String str = "@" + id + " = " + "dso_local global [" + size1 + " x [" + size2 + " x i32]] [";
                for (int i = 0; i < size1; i++) {
                    str += "[" + size2 + " x i32] [";
                    for (int j = 0; j < size2; j++) {
                        str += "i32 " + vals.get(i * size2 + j) + ",";
                    }
                    str = str.substring(0, str.length() - 1) + "],";
                }
                str = str.substring(0, str.length() - 1) + "]";
                System.out.println(str);
                Symbol s = new Symbol(id, size1, size2, vals, p.getChild().getToken().getLn(), "@" + id);
                if (curTable.findHere(id) == null) {
                    curTable.putSym(s);
                } else {
                    new MyException("b", s.getLn());
                }
            } else {
                System.out.println("%v" + regId++ + " = alloca [" + size1 + " x [" + size2 + " x i32]]");
                Symbol s = new Symbol(id, size1, size2, vals, p.getChild().getToken().getLn(), "%v" + (regId-1));
                if (curTable.findHere(id) == null) {
                    curTable.putSym(s);
                } else {
                    new MyException("b", s.getLn());
                }
                System.out.println("%v" + regId + " = getelementptr " + "[" + size1 + " x [" + size2 + " x i32]]" + ", " + "[" + size1 + " x [" + size2 + " x i32]]" + "* %v" + (regId-1) + ", i32 0, i32 0");
                regId++;
                System.out.println("%v" + regId + " = getelementptr [" + size2 + " x i32], [" + size2 + " x i32]* " + "%v" + (regId-1) + ", i32 0, i32 0");
                regId++;
                String head = "%v" + (regId-1);
                //System.out.println("call void @memset(i32* %v" + (regId-1) + ", i32 0, i32 " + size1*size2*4 + ")");
                for (int i=0; i<size1*size2; i++) {
                    System.out.println("%v" + regId + " = getelementptr i32, i32* " + head + ", i32 " + i);
                    regId++;
                    String val = vals.get(i);
                    System.out.println("store i32 " + val + ", i32* %v" + (regId-1));
                }
            }
        }
    }
    
    private static ArrayList<String> InitVal(GrammarTreeNode p) {
        if (p.getChildType(0) == TokenType.Exp) {  // 普通初值
            ArrayList<String> ret = new ArrayList<>();
            ret.add(Exp(p.getChild()).getVal());
            return ret;
        }
        else {  // 数组初值
            ArrayList<String> ret = new ArrayList<>();
            for (int i=0; i<p.getChildsSize(); i++) {
                GrammarTreeNode c = p.getChild(i);
                if (c.getType() == TokenType.InitVal) {
                    ret.addAll(InitVal(c));
                }
                else if (c.getType() != TokenType.RBRACE){
                    continue;
                }
                else return ret;
            }
            return null;
        }
    }
    
    private static ArrayList<String> ConstInitVal(GrammarTreeNode p) {  // 按一维数组格式返回
        if (p.getChildType(0) == TokenType.ConstExp) {  // 普通初值
            ArrayList<String> ret = new ArrayList<>();
            String s = ConstExp(p.getChild()).getVal();
            ret.add(s);
            return ret;
        }
        else {  // 数组初值
            ArrayList<String> ret = new ArrayList<>();
            for (int i=0; i<p.getChildsSize(); i++) {
                GrammarTreeNode c = p.getChild(i);
                if (c.getType() == TokenType.ConstInitVal) {
                    ret.addAll(ConstInitVal(c));
                }
                else if (c.getType() != TokenType.RBRACE){
                    continue;
                }
                else return ret;
            }
            return null;
        }
    }
    
    private static void MainFuncDef(GrammarTreeNode p) {
        hasRet = false;
        System.out.print("define i32 @main() {\n");
        regId = 0;
        FuncTable func = new FuncTable(TokenType.INTTK);
        func.setParent(curTable);
        funcTables.put("main", func);
        curTable = func;
        isVoid = false;
        Block(p.getChild(4));
        GrammarTreeNode block = p.getChild(p.getChildsSize() - 1);
        GrammarTreeNode last = block.getChild(block.getChildsSize() - 2);
        if ((last.getType() != TokenType.BlockItem || last.getChild().getType() != TokenType.Stmt || last.getChild().getChild().getType() != TokenType.RETURNTK)) {
            new MyException("g", last.getNext().getToken().getLn());
        }
        curTable = curTable.goOut();
        System.out.print("}\n");
    }
    
    private static void FuncDef(GrammarTreeNode p){
        String funcType = p.getChild().getChildType(0) == TokenType.VOIDTK ? "void" : "i32";
        isVoid = funcType.equals("void");
        String id = p.getChild(1).getVal();
        hasRet = false;
        firstRetLine = 0;
        if (funcTables.containsKey(id) || globalTable.findHere(id) != null) {   // 重名函数
            new MyException("b", p.getChild(1).getToken().getLn());
        }
        FuncTable ft = new FuncTable(p.getChild().getChildType(0));
        ft.setParent(curTable);
        if (!funcTables.containsKey(id) && globalTable.findHere(id) == null) {
            funcTables.put(id, ft);
        }
        curTable = ft;
        regId = 0;
        if (p.getChildsSize() == 6) {   // 有形参
            System.out.print("define " + funcType + " @" + id + "(");
            FuncFParams(p.getChild(3));
            regId = 0;
            System.out.print(") {\n");
            for (int i=0 ;i<ft.getParams().size(); i++) {
                System.out.print("%v" + regId + " = alloca " + ft.getParams().get(i).getParamType() +  "\n");
                System.out.print("store " +  ft.getParams().get(i).getParamType() +  " %" + i + ", " + ft.getParams().get(i).getParamType() + " * %v" + regId + "\n");
                ft.getParams().get(i).setRegName("%v" + regId);
                ft.getParams().get(i).setVal("%v" + regId);
                ++regId;
            }
            Block(p.getChild(5));
        }
        else {
            System.out.print("define " + funcType + " @" + id + "() {\n");
            Block(p.getChild(4));
        }
        if (funcType.equals("void")) {
            System.out.print("ret void\n");
        }
        System.out.print("}\n\n");
        GrammarTreeNode block = p.getChild(p.getChildsSize() - 1);
        GrammarTreeNode last = block.getChild(block.getChildsSize() - 2);
        if (funcType.equals("i32") && (last.getType() != TokenType.BlockItem || last.getChild().getType() != TokenType.Stmt || last.getChild().getChild().getType() != TokenType.RETURNTK)) {
            new MyException("g", last.getNext().getToken().getLn());
        }
        curTable = curTable.goOut();
    }
    
    private static void FuncFParams(GrammarTreeNode p) {
        int paramCnt = (p.getChildsSize() + 1) / 2;
        int i;
        for (i=0; i<paramCnt-1; i++) {
            FuncFParam(p.getChild(2*i));
            System.out.print(", ");
        }
        FuncFParam(p.getChild(2*i));
    }
    
    private static void FuncFParam(GrammarTreeNode p) {
        String id = p.getChild(1).getVal();
        if (curTable.findHere(id) != null) {    // 重名,报错，补充一个占位参数
            new MyException("b", p.getChild(1).getToken().getLn());
            Symbol s = new Symbol("^error"+regId, SymClass.PARA, p.getChild(1).getToken().getLn(), "%v" + regId);
            regId++;
            curTable.putSym(s);
        }
        else {
            if (p.getChildsSize() == 2) {   // 普通参数
                System.out.print("i32");
                Symbol s = new Symbol(id, SymClass.PARA, p.getChild(1).getToken().getLn(), "%v" + regId);
                s.setParamType("i32");
                regId++;
                curTable.putSym(s);
            }
            else if (p.getChildsSize() == 4) {   // 一维数组参数
                System.out.print("i32*");
                Symbol s = new Symbol(id, 1, 1, SymClass.ARR1_PARA, p.getChild(1).getToken().getLn());
                s.setParamType("i32*");
                curTable.putSym(s);
            }
            else if (p.getChildsSize() == 7) {   // 二维数组参数
                int size2 = Integer.parseInt(ConstExp(p.getChild(5)).getVal());
                System.out.print("[" + size2 + " x i32]*");
                Symbol s = new Symbol(id, 2, size2, SymClass.ARR2_PARA, p.getChild(1).getToken().getLn());
                s.setParamType("[" + size2 + " x i32]*");
                curTable.putSym(s);
            }
        }
    }
    
    private static void Block(GrammarTreeNode p) {
        for (int i=1; i<p.getChildsSize()-1; i++) {
            BlockItem(p.getChild(i));
        }
    }
    
    private static void BlockItem(GrammarTreeNode p) {
        GrammarTreeNode c = p.getChild();
        if (c.getType() == TokenType.Decl) {
            Decl(c);
        }
        else {
            Stmt(c);
        }
    }
    
    private static void Stmt(GrammarTreeNode p) {
        GrammarTreeNode c = p.getChild();
        switch ( c.getType()) {
            case LVal:
                Symbol lval = LVal(c);
                String id = c.getChild().getVal();
                if (lval.isConst()) {
                    new MyException("h", c.getNext().getToken().getLn());
                    return;
                }
                if (lval != null && !lval.isConst()) {
                    GrammarTreeNode r = p.getChild(2);  // Exp or getint()
                    if (r.getType() == TokenType.Exp) {
                        String var = Exp(r).getVal();
                        System.out.print("store i32 " + var + ", i32* " + lval.getRegName() + "\n");
                    } else {
                        System.out.print("%v" + regId + " = call i32 @getint()\n");
                        System.out.print("store i32 %v" + regId + ", i32* " + lval.getRegName() + "\n");
                        regId++;
                    }
                }
                break;
            case Exp:
                Exp(c);
                System.out.print("\n");
                break;
            case Block:
                SymTable son = new SymTable();
                son.setParent(curTable);
                curTable = son;
                Block(c);
                curTable = curTable.goOut();
                break;
            case RETURNTK:
                hasRet = true;
                if (firstRetLine == 0) {
                    firstRetLine = c.getToken().getLn();
                }
                if (c.getNext().getType() == TokenType.Exp) {   // 有返回值
                    if (isVoid) {
                        new MyException("f", c.getToken().getLn());
                        return;
                    }
                    String r = Exp(c.getNext()).getVal();
                    System.out.print("ret i32 " + r + "\n");
                }
                else {
                    System.out.print("ret void\n");
                }
                break;
            case PRINTFTK:
                GrammarTreeNode gtn = c.getNext().getNext();
                String format = gtn.getVal();
                ArrayList<String> exps = new ArrayList<>();
                gtn = gtn.getNext().getNext();
                while (!(gtn.getType() == TokenType.SEMICN)) {
                    exps.add(Exp(gtn).getVal());
                    gtn = gtn.getNext().getNext();
                }
                int expIdx = 0;
                int dCnt = format.split("%d").length - 1;
                if (dCnt != exps.size()) {
                    new MyException("l", c.getToken().getLn());
                    return;
                }
                // for (int i = 0; i < exps.size(); i++) System.out.println("exp[" + i + "] = " + exps.get(i));
                char[] chars = format.substring(1, format.length()-1).toCharArray(); // 去除头尾双引号
                for (int i=0; i<chars.length; i++) {
                    if (chars[i] == '\\' && i < chars.length-1 &&chars[i+1] == 'n') {
                        System.out.print("call void @putch(i32 10)\n");   // 换行
                        i = i + 1;
                    }
                    else if (chars[i] == '%' && i < chars.length-1 && chars[i+1] == 'd') {
                        System.out.print("call void @putint(i32 " + exps.get(expIdx++) + ")\n");   // %d
                        i = i + 1;
                    }
                    else if (chars[i] == 32 || chars[i] == 33 || (chars[i] >= 40 && chars[i] <= 91) || (chars[i] >= 93 && chars[i] <= 126)) {
                        System.out.print("call void @putch(i32 " + (int)chars[i] + ")\n");
                    }
                    else {
                        new MyException("a", c.getToken().getLn());
                        return;
                    }
                }
                break;
            case BREAKTK:
                if (loopDepth <= 0) {
                    new MyException("m", c.getToken().getLn());
                }
                Loop curLoop = loopStack.get(loopStack.size()-1);
                System.out.println("br label " + curLoop.getOutLabel());
                break;
            case CONTINUETK:
                if (loopDepth <= 0) {
                    new MyException("m", c.getToken().getLn());
                }
                Loop cLoop = loopStack.get(loopStack.size()-1);
                System.out.println("br label " + cLoop.getCondLabel());
                break;
            case IFTK:
                if (p.getChildsSize() == 5) {   // no else
                    String iflabel = "%v" + regId;
                    regId++;
                    String endlabel = "%v" + regId;
                    regId++;
                    Symbol cond = Cond(p.getChild(2), iflabel, null, endlabel);
                    System.out.println("br i1 " + cond.getVal() + " ,label " + iflabel + ",label " + endlabel);
                    System.out.println(iflabel.substring(1,iflabel.length()) + ":");
                    Stmt(p.getChild(4));
                    System.out.println("br label " + endlabel);
                    System.out.println(endlabel.substring(1,endlabel.length()) + ":");
                } else {
                    String iflabel = "%v" + regId;
                    regId++;
                    String elselabel = "%v" + regId;
                    regId++;
                    String endlabel = "%v" + regId;
                    regId++;
                    Symbol cond = Cond(p.getChild(2), iflabel, elselabel, endlabel);
                    System.out.println("br i1 " + cond.getVal() + " ,label " + iflabel + ",label " + elselabel);
                    System.out.println(iflabel.substring(1,iflabel.length()) + ":");
                    Stmt(p.getChild(4));
                    System.out.println("br label " + endlabel);
                    System.out.println(elselabel.substring(1,elselabel.length()) + ":");
                    Stmt(p.getChild(6));
                    System.out.println("br label " + endlabel);
                    System.out.println(endlabel.substring(1,endlabel.length()) + ":");
                }
                break;
            case WHILETK:
                String condLabel = "%v" + regId++;
                String bodyLabel = "%v" + regId++;
                String outLabel = "%v" + regId++;
                System.out.println("br label " + condLabel);
                System.out.println(condLabel.substring(1,condLabel.length()) + ":");
                loopStack.add(new Loop(condLabel, bodyLabel, outLabel));
                loopDepth++;
                Symbol cond = Cond(p.getChild(2), bodyLabel, null, outLabel);
                System.out.println("br i1 " + cond.getVal() + " ,label " + bodyLabel + ",label " + outLabel);
                System.out.println(bodyLabel.substring(1,bodyLabel.length()) + ":");
                Stmt(p.getChild(4));
                System.out.println("br label " + condLabel);
                System.out.println(outLabel.substring(1,outLabel.length()) + ":");
                loopStack.remove(loopStack.size()-1);
                loopDepth--;
                break;
            default: ;
        }
    }
    
    private static Symbol Cond(GrammarTreeNode p, String iflabel, String elselabel, String endlabel) {
        return LOrExp(p.getChild(), iflabel, elselabel, endlabel);
    }
    
    private static Symbol LOrExp(GrammarTreeNode p, String iflabel, String elselabel, String endlabel) {
        int LAndCnt = (p.getChildsSize() + 1) / 2;
        String nextOrLabel = "%v" + regId;
        regId++;
        if (LAndCnt == 1) {
            nextOrLabel = elselabel == null ? endlabel : elselabel;
        }
        Symbol l = LAndExp(p.getChild(0), nextOrLabel);
        for (int i=1; i<LAndCnt; i++) {
            if (l.getIs1Bit()) {
                System.out.println("%v" + regId + " = zext i1 " + l.getVal() + " to i32");
                l.setVal("%v" + regId);
                l.setIs1Bit(false);
                regId++;
            }
            System.out.println("%v" + regId + " = icmp " + "ne" + " i32 " + l.getVal() + ", " + "0");
            regId++;
            System.out.println("br i1 %v" + (regId-1) + " ,label " + iflabel + ",label " + nextOrLabel);
            System.out.println(nextOrLabel.substring(1,nextOrLabel.length()) + ":");
            nextOrLabel = "%v" + regId;
            regId++;
            if (i == LAndCnt - 1) {
                nextOrLabel = elselabel == null ? endlabel : elselabel;
            }
            Symbol r = LAndExp(p.getChild(i * 2), nextOrLabel);
            if (r.getIs1Bit()) {
                System.out.println("%v" + regId + " = zext i1 " + r.getVal() + " to i32");
                r.setVal("%v" + regId);
                r.setIs1Bit(false);
                regId++;
            }
            
            l = new Symbol("bool", r.getVal(), SymClass.VAR, -1);
            l.setIs1Bit(r.getIs1Bit());
        }
        if (l.getIs1Bit()) {
            System.out.println("%v" + regId + " = zext i1 " + l.getVal() + " to i32");
            l.setVal("%v" + regId);
            l.setIs1Bit(false);
            regId++;
        }
        System.out.println("%v" + regId + " = icmp " + "ne" + " i32 " + l.getVal() + ", " + "0");
        regId++;
        Symbol ret = new Symbol("bool", "%v"+(regId-1), SymClass.VAR, -1);
        ret.setIs1Bit(true);
        return ret;
    }
    
    private static Symbol LAndExp(GrammarTreeNode p, String nextOrLabel) {
        int EqCnt = (p.getChildsSize() + 1) / 2;
        Symbol l = EqExp(p.getChild(0));
        for (int i=1; i<EqCnt; i++) {
            if (l.getIs1Bit()) {
                System.out.println("%v" + regId + " = zext i1 " + l.getVal() + " to i32");
                l.setVal("%v" + regId);
                l.setIs1Bit(false);
                regId++;
            }
            System.out.println("%v" + regId + " = icmp " + "eq" + " i32 " + l.getVal() + ", " + "0");
            regId++;

            System.out.println("br i1 %v" + (regId - 1) + " ,label " + nextOrLabel + ",label " + "%v" + regId);
            System.out.println("v" + regId + ":");
            regId++;

            Symbol r = EqExp(p.getChild(i * 2));
            if (r.getIs1Bit()) {
                System.out.println("%v" + regId + " = zext i1 " + r.getVal() + " to i32");
                r.setVal("%v" + regId);
                r.setIs1Bit(false);
                regId++;
            }
    
            System.out.println("%v" + regId + " = and i32 " + l.getVal() + ", " + r.getVal());
            regId++;
            l = new Symbol("bool", "%v"+(regId-1), SymClass.VAR, -1);
        }
        return l;
    }
    
    private static Symbol EqExp(GrammarTreeNode p) {
        int RelCnt = (p.getChildsSize() + 1) / 2;
        Symbol l = RelExp(p.getChild(0));
        for (int i=1; i<RelCnt; i++) {
            Token op = p.getChild(i * 2 - 1).getToken();
            Symbol r = RelExp(p.getChild(i * 2));
            if (l.getIs1Bit()) {
                System.out.println("%v" + regId + " = zext i1 " + l.getVal() + " to i32");
                l.setVal("%v" + regId);
                l.setIs1Bit(false);
                regId++;
            }
            if (r.getIs1Bit()) {
                System.out.println("%v" + regId + " = zext i1 " + r.getVal() + " to i32");
                r.setVal("%v" + regId);
                r.setIs1Bit(false);
                regId++;
            }
            System.out.println("%v" + regId + " = icmp " + condMap.get(op.getType()) + " i32 " + l.getVal() + ", " + r.getVal());
            regId++;
            l = new Symbol("bool", "%v"+(regId-1), SymClass.VAR, -1);
            l.setIs1Bit(true);
        }
        if (l.getIs1Bit()) {
            System.out.println("%v" + regId + " = zext i1 " + l.getVal() + " to i32");
            l.setVal("%v" + regId);
            l.setIs1Bit(false);
            regId++;
        }
        System.out.println("%v" + regId + " = icmp " + "ne" + " i32 " + l.getVal() + ", " + "0");
        regId++;
        l.setVal("%v" + (regId-1));
        l.setIs1Bit(true);
        return l;
    }

    private static Symbol RelExp(GrammarTreeNode p) {
        int AddCnt = (p.getChildsSize() + 1) / 2;
        Symbol l = AddExp(p.getChild(0));
        if (l.getIs1Bit()) {
            System.out.println("%v" + regId + " = zext i1 " + l.getVal() + " to i32");
            l.setVal("%v" + regId);
            l.setIs1Bit(false);
            regId++;
        }
        for (int i=1; i<AddCnt; i++) {
            Token op = p.getChild(i * 2 - 1).getToken();
            Symbol r = AddExp(p.getChild(i * 2));
            if (l.getIs1Bit()) {
                System.out.println("%v" + regId + " = zext i1 " + l.getVal() + " to i32");
                l.setVal("%v" + regId);
                l.setIs1Bit(false);
                regId++;
            }
            if (r.getIs1Bit()) {
                System.out.println("%v" + regId + " = zext i1 " + r.getVal() + " to i32");
                r.setVal("%v" + regId);
                r.setIs1Bit(false);
                regId++;
            }
            System.out.println("%v" + regId + " = icmp " + condMap.get(op.getType()) + " i32 " + l.getVal() + ", " + r.getVal());
            regId++;
            l = new Symbol("bool", "%v"+(regId-1), SymClass.VAR, -1);
            l.setIs1Bit(true);
        }
        return l;
    }
    
    private static Symbol LVal(GrammarTreeNode p) {
        Symbol sym = curTable.findName(p.getChild().getVal());
        if (sym == null) {  // 使用未定义ident
            new MyException("c", p.getChild().getToken().getLn());
            Symbol ret = new Symbol("ret", "4444", -1);
            return ret;
        }
        if (p.getChildsSize() == 1) {   // 普通变量
            GrammarTreeNode c = p.getChild();
            Symbol ret;
            if (sym.isConst() && sym.getDimension() == 0) {
                ret = new Symbol(sym.getName(), sym.getVal(), SymClass.VAR, -1, true);
                return ret;
            }
            else if (sym.getDimension() == 0){  // 普通变量 a
               //System.out.print("%v" + regId + " = load i32, i32* " + sym.getRegName() + "\n");
               //regId++;
               //Symbol r = new Symbol("ret", "%v" + (regId - 1), SymClass.VAR, -1);
               return sym;
            } else if (sym.getDimension() == 2 && sym.getParamType() != null) { // 二维数组传参
                System.out.println("%v" + regId + " = load " + sym.getParamType() + ", " + sym.getParamType() + " * " + sym.getRegName());
                regId++;
                Symbol r = new Symbol(sym.getName(), "%v" + (regId - 1), SymClass.VAR, -1);
                r.setRegName("%v"+(regId-1));
                r.setDimension(2);
                return r;
            } else if (sym.getDimension() == 2 && sym.getParamType() == null) {
                 int size1 = sym.getLen1();
                int size2 = sym.getLen2();
                System.out.println("%v" + regId + " = getelementptr [" + size1 + " x [" + size2 + " x i32]], [" + size1 + " x [" + size2 + " x i32]]* " + sym.getRegName() + ", i32 0, i32 0");
                regId++;
                ret = new Symbol(sym.getName(), "%v" + (regId - 1), SymClass.VAR, -1);
                ret.setRegName("%v"+(regId-1));
                ret.setIsConst(sym.isConst());
                ret.setDimension(2);
                return ret;
            }
            else if (sym.getParamType() != null){
                /*int size1 = sym.getLen1();
                System.out.println("%v" + regId + " = getelementptr [" + size1 + " x i32], [" + size1 + " x i32]* " + sym.getRegName() + ", i32 0, i32 0");
                regId++;
                ret = new Symbol(sym.getName(), "%v" + (regId - 1), SymClass.VAR, -1);
                ret.setRegName("%v"+(regId-1));
                ret.setIsConst(sym.isConst());
                ret.setDimension(1);
                return ret;*/
                System.out.println("%v" + regId + " = load i32*, i32* * " + sym.getRegName());
                regId++;
                Symbol r = new Symbol(sym.getName(), "%v" + (regId - 1), SymClass.VAR, -1);
                r.setRegName("%v"+(regId-1));
                r.setDimension(1);
                return r;
            } else {
                int size1 = sym.getLen1();
                System.out.println("%v" + regId + " = getelementptr [" + size1 + " x i32], [" + size1 + " x i32]* " + sym.getRegName() + ", i32 0, i32 0");
                regId++;
                Symbol r = new Symbol(sym.getName(), "%v" + (regId - 1), SymClass.VAR, -1);
                r.setRegName("%v"+(regId-1));
                r.setDimension(1);
                return r;
            }
        } else if (p.getChildsSize() == 4) { // a[i]
            GrammarTreeNode c = p.getChild();
            Symbol i_sym = Exp((p.getChild(2)));
            sym = curTable.findName(c.getVal());
            Symbol ret;
            if (sym.getDimension() == 0) {
                return new Symbol("nullVals", "0", -1);
            }
            else if (sym.getDimension() == 1 && sym.getParamType() == null) {
                if (sym.isConst() && i_sym.isConst()) {
                    int i = Integer.parseInt(i_sym.getVal());
                    Symbol r = new Symbol(sym.getName(), sym.getValArray().get(i), SymClass.VAR, -1, true);
                    return r;
                }
                else {
                    int size1 = sym.getLen1();
                    System.out.println("%v" + regId + " = getelementptr [" + size1 + " x i32], [" + size1 + " x i32]* " + sym.getRegName() + ", i32 0, i32 " + i_sym.getVal());
                    regId++;
                    Symbol r = new Symbol(sym.getName(), "%v" + (regId - 1), SymClass.VAR, -1, sym.isConst());
                    r.setRegName("%v" + (regId - 1));
                    return r;
                }
            }
            else if (sym.getDimension() == 1 && sym.getParamType().equals("i32*")) {
                System.out.println("%v" + regId + " = load i32*, i32* * " + sym.getRegName());
                regId++;
                System.out.println("%v" + regId + " = getelementptr i32, i32* " + "%v" + (regId-1) + ", i32 " + i_sym.getVal());
                regId++;
                Symbol r = new Symbol(sym.getName(), "%v" + (regId - 1), SymClass.VAR, -1);
                r.setRegName("%v"+(regId-1));
                return r;
            }
            else if (sym.getDimension() == 2 && sym.getParamType() == null){
                int size1 = sym.getLen1();
                int size2 = sym.getLen2();
                System.out.println("%v" + regId + " = getelementptr [" + size1 + " x [" + size2 + " x i32]], [" + size1 + " x [" + size2 + " x i32]]* " + sym.getRegName() + ", i32 0, i32 " + i_sym.getVal());
                regId++;
                System.out.println("%v" + regId + " = getelementptr [" + size2 + " x i32], [" + size2 + " x i32]* " + "%v" + (regId-1) + ", i32 0, i32 0");
                regId++;
                ret = new Symbol(sym.getName(), "%v" + (regId - 1), SymClass.VAR, -1, sym.isConst());
                ret.setDimension(1);
                ret.setRegName("%v"+(regId-1));
                return ret;
            } else {
                System.out.println("%v" + regId + " = load " + sym.getParamType() + ", " + sym.getParamType() + " * " + sym.getRegName());
                regId++;
                String ty = sym.getParamType();
                System.out.println("%v" + regId + " = getelementptr " + ty.substring(0,ty.length()-1) + ", " + ty + " %v" + (regId-1) + ", i32 " + i_sym.getVal());
                regId++;
                String head = "%v" + (regId-1);
                System.out.println("%v" + regId + " = getelementptr " + ty.substring(0,ty.length()-1) + ", " + ty + " " + head + ", i32 0, i32 0");
                regId++;
                Symbol r = new Symbol(sym.getName(), "%v" + (regId - 1), SymClass.VAR, -1);
                r.setRegName("%v"+(regId-1));
                r.setDimension(1);
                return r;
            }
        } else {    // a[1][2]
            GrammarTreeNode c = p.getChild();
            Symbol i_sym = Exp((p.getChild(2)));
            Symbol j_sym = Exp((p.getChild(5)));
            sym = curTable.findName(c.getVal());
            Symbol ret;
            if (sym.getDimension() != 2) {
                return new Symbol("nullVals", "0", -1);
            }
            else if (sym.getParamType() == null){
                int size1 = sym.getLen1();
                int size2 = sym.getLen2();
                if (sym.isConst() && i_sym.isConst() && j_sym.isConst()) {
                    int i = Integer.parseInt(i_sym.getVal());
                    int j = Integer.parseInt(j_sym.getVal());
                    Symbol r = new Symbol(sym.getName(), sym.getValArray().get(i*size2+j), SymClass.VAR, -1, true);
                    return r;
                }
                else {
                    System.out.println("%v" + regId + " = getelementptr [" + size1 + " x [" + size2 + " x i32]], [" + size1 + " x [" + size2 + " x i32]]* " + sym.getRegName() + ", i32 0, i32 " + i_sym.getVal());
                    regId++;
                    System.out.println("%v" + regId + " = getelementptr [" + size2 + " x i32], [" + size2 + " x i32]* " + "%v" + (regId - 1) + ", i32 0, i32 " + j_sym.getVal());
                    regId++;
                    Symbol r = new Symbol(sym.getName(), "%v" + (regId - 1), SymClass.VAR, -1, sym.isConst());
                    r.setRegName("%v" + (regId - 1));
                    return r;
                }
            }
            else {
                System.out.println("%v" + regId + " = load " + sym.getParamType() + ", " + sym.getParamType() + " * " + sym.getRegName());
                regId++;
                String[] s = sym.getParamType().split(" ");
                int size2 = Integer.parseInt(s[0].substring(1,s[0].length()));
                String ty = sym.getParamType();
                System.out.println("%v" + regId + " = getelementptr " + ty.substring(0,ty.length()-1) + ", " + ty + " %v" + (regId-1) + ", i32 0");
                regId++;
                String head = "%v" + (regId-1);
                System.out.println("%v" + regId + " = mul i32 " + i_sym.getVal() + ", " + size2);
                regId++;
                System.out.println("%v" + regId + " = add i32 " + "%v" + (regId-1) + ", " + j_sym.getVal());
                regId++;
                System.out.println("%v" + regId + " = getelementptr " + ty.substring(0,ty.length()-1) + ", " + ty + " " + head + ", i32 0, i32 " + "%v" + (regId-1));
                regId++;
                Symbol r = new Symbol(sym.getName(), "%v" + (regId - 1), SymClass.VAR, -1);
                r.setRegName("%v"+(regId-1));
                return r;
            }
        }
    }
    
    private static Symbol PrimaryExp(GrammarTreeNode p) {
        GrammarTreeNode c = p.getChild();
        if (c.getType() == TokenType.LVal) {
            Symbol sym = LVal(c);
            if (sym.getDimension() == 0) {
                if (!sym.isConst()) {
                    System.out.print("%v" + regId + " = load i32, i32* " + sym.getRegName() + "\n");
                    regId++;
                    Symbol r = new Symbol("ret", "%v" + (regId - 1), SymClass.VAR, -1);
                    return r;
                }
                if (sym.getRegName() != null) {
                    System.out.print("%v" + regId + " = load i32, i32* " + sym.getRegName() + "\n");
                    regId++;
                    Symbol r = new Symbol("ret", "%v" + (regId - 1), SymClass.VAR, -1);
                    return r;
                }
            } //else 数组指针，直接传上去
            return sym;
        }
        else if (c.getType() == TokenType.Number) {
            Symbol num = new Symbol("ret", c.getChild().getVal().toString(), SymClass.VAR, -1, true);
            return num;
        }
        else {  // （EXP）
            return Exp(c.getNext());
        }
    }
    
    private static Symbol UnaryExp(GrammarTreeNode p) {
        GrammarTreeNode c = p.getChild();
        if (c.getType() == TokenType.PrimaryExp) {
            return PrimaryExp(c);
        }
        else if (c.getType() == TokenType.IDENFR && c.getNext().getNext().getType() == TokenType.FuncRParams) { // 带参调用
            ArrayList<Symbol> vars = FuncRParams(c.getNext().getNext());
            /*for (String var: vars) {
                if (var.startsWith("%")) {
                    System.out.print("%" + regId + " = load i32, i32* " + var + "\n");
                }
                else System.out.print("store i32 " + var + ", i32* %" + regId + "\n");
                regId++;
            }*/
            if (!funcTables.containsKey(c.getVal()) || curTable.findName(c.getVal()) != null) { // 使用未定义ident
                new MyException("c", c.getToken().getLn());
                Symbol ret = new Symbol("ret", "44444", -1);
                return ret;
            }
            if (funcTables.get(c.getVal()).getParams().size() != vars.size()) {
                new MyException("d", c.getToken().getLn());
                Symbol ret = new Symbol("ret", "444444", -1);
                return ret;
            }
            TokenType returnType = funcTables.get(c.getVal()).getReturnType();
            String type = returnType == TokenType.VOIDTK ? "void" : "i32";
            String callParams = "(";
            ArrayList<Symbol> params = funcTables.get(c.getVal()).getParams();
            for (int i = 0; i < params.size(); i++) {
                if (params.get(i).getDimension() != vars.get(i).getDimension()) {   // 形参实参维数不一致
                    new MyException("e", c.getToken().getLn());
                    Symbol ret = new Symbol("ret", "33355", -1);
                    return ret;
                }
                if (params.get(i).getSymClass() == SymClass.PARA || params.get(i).getSymClass() == SymClass.ARR2_PARA || params.get(i).getSymClass() == SymClass.ARR1_PARA) {
                    callParams = callParams + params.get(i).getParamType() + " " + vars.get(i).getVal() + ", "; // 代码生成2
                }
            }
            callParams = callParams.substring(0, callParams.length() - 2) + ")";
            if (!type.equals("void")) {
                System.out.print("%v" + regId + " = call " + type + " @" + c.getVal() + callParams + "\n");
                regId++;
            }
            else {
                System.out.print("call " + type + " @" + c.getVal() + callParams + "\n");
            }
            Symbol ret = new Symbol("ret", "%v" + (regId-1), SymClass.VAR, -1);
            if (funcTables.get(c.getVal()).getReturnType() == TokenType.VOIDTK) {
                ret.setDimension(-1);
            }
            return ret;
        }
        else if (c.getType() == TokenType.IDENFR) {     // 无参调用
            if (!funcTables.containsKey(c.getVal()) || curTable.findName(c.getVal()) != null) { // 使用未定义ident
                new MyException("c", c.getToken().getLn());
                Symbol ret = new Symbol("ret", "44444", -1);
                return ret;
            }
            if (funcTables.get(c.getVal()).getParams().size() != 0) {
                new MyException("d", c.getToken().getLn());
                Symbol ret = new Symbol("ret", "999444", -1);
                return ret;
            }
            TokenType returnType = funcTables.get(c.getVal()).getReturnType();
            String type = returnType == TokenType.VOIDTK ? "void" : "i32";
            if (!type.equals("void")) {
                System.out.print("%v" + regId + " = call " + type + " @" + c.getVal() + "()\n");
                regId++;
            }
            else {
                System.out.print("call " + type + " @" + c.getVal() + "()\n");
            }
            Symbol ret = new Symbol("ret", "%v" + (regId-1), SymClass.VAR, -1);
            if (funcTables.get(c.getVal()).getReturnType() == TokenType.VOIDTK) {
                ret.setDimension(-1);
            }
            return ret;
        }
        else {  // +-!UnaryExp
            Symbol s = UnaryExp(c.getNext());
            if (c.getChildType(0) == TokenType.NOT) {
                if (s.getIs1Bit()) {
                    System.out.println("%v" + regId + " = zext i1 " + s.getVal() + " to i32");
                    s.setVal("%v" + regId);
                    s.setIs1Bit(false);
                    regId++;
                }
                System.out.println("%v" + regId++ + " = icmp " + "ne" + " i32 " + s.getVal() + ", " + "0");
                System.out.println("%v" + regId + " = xor " + "i1 " + "%v" + (regId-1) + ", " + "true");
                regId++;
                System.out.println("%v" + regId + " = zext i1 " + "%v" + (regId-1) + " to i32");
                regId++;
                Symbol ret = new Symbol("ret", "%v" + (regId-1), SymClass.VAR, -1);
                return ret;
            }
            else if (c.getChildType(0) == TokenType.PLUS) {
                return UnaryExp(c.getNext());
            }
            else if (c.getChildType(0) == TokenType.MINU) {
                String var = UnaryExp(c.getNext()).getVal();
                if (var.startsWith("%")) {
                    System.out.print("%v" + regId + " = sub i32 0, " + var + "\n");
                    regId++;
                    Symbol ret = new Symbol("ret", "%v" + (regId - 1), SymClass.VAR, -1);
                    return ret;
                }
                else return new Symbol("ret", (0 - Integer.parseInt(var)) + "", SymClass.VAR, -1, true);
            }
            else return null;
        }
    }
    
    private static ArrayList<Symbol> FuncRParams(GrammarTreeNode p) {
        GrammarTreeNode c = p.getChild();
        int expCnt = (p.getChildsSize() + 1) / 2;
        ArrayList<Symbol> ret = new ArrayList<>();
        for (int i=0; i<expCnt-1; i++) {
            ret.add(Exp(c));
            c = c.getNext().getNext();
        }
        ret.add(Exp(c));
        return ret;
    }
    
    private static Symbol MulExp(GrammarTreeNode p) {
        GrammarTreeNode c = p.getChild();
        ArrayList<Symbol> unaryExps = new ArrayList<>();
        unaryExps.add(UnaryExp(c));
        c = c.getNext();
        while (c != null) {
            unaryExps.add(new Symbol("temp", c.getVal(), SymClass.VAR, -1)); // add op
            c = c.getNext();
            unaryExps.add(UnaryExp(c));    // add oprand
            c = c.getNext();
        }
        // unaryExp为指针或为void，说明这是函数参数
        if (unaryExps.get(0).getDimension() != 0) {
            return unaryExps.get(0);
        }
        // 否则unaryExp是用于计算的表达式
        String left = unaryExps.get(0).getVal();
        String op, right;
        for (int i = 1; i < unaryExps.size(); i++) {
            op = unaryExps.get(i).getVal();
            ++i;
            right = unaryExps.get(i).getVal();
            if (!left.startsWith("%") && !right.startsWith("%")) { // 双常量
                switch (op) {
                    case "*": {
                        left = Integer.parseInt(left) * Integer.parseInt(right) + "";
                        break;
                    }
                    case "/": {
                        left = Integer.parseInt(left) / Integer.parseInt(right) + "";
                        break;
                    }
                    case "%": {
                        left = Integer.parseInt(left) % Integer.parseInt(right) + "";
                        break;
                    }
                    default:;
                }
            }
            else {
                switch (op) {
                    case "*": {
                        System.out.print("%v" + regId + " = mul i32 " + left + ", " + right + "\n");
                        break;
                    }
                    case "/": {
                        System.out.print("%v" + regId + " = sdiv i32 " + left + ", " + right + "\n");
                        break;
                    }
                    case "%": {
                        System.out.print("%v" + regId + " = srem i32 " + left + ", " + right + "\n");
                        break;
                    }
                    default:;
                }
                left = "%v" + regId;
                regId++;
            }
        }
        if (debug) System.out.print("MulExp returns " + left + "\n");
        Symbol ret = new Symbol("ret", left, SymClass.VAR, -1, !left.startsWith("%"));
        return ret;
    }
    
    private static Symbol AddExp(GrammarTreeNode p) {
        GrammarTreeNode c = p.getChild();
        // MulExp为指针或为void，说明这是函数参数
        Symbol mulExp = MulExp(c);
        if (mulExp.getDimension() != 0) {
            return mulExp;
        }
        // 否则MulExp是用于计算的表达式
        ArrayList<String> mulExps = new ArrayList<>();
        mulExps.add(mulExp.getVal());
        c = c.getNext();
        while (c != null) {
            mulExps.add(c.getVal());
            c = c.getNext();
            mulExps.add(MulExp(c).getVal());
            c = c.getNext();
        }
        String left = mulExps.get(0);
        String op, right;
        for (int i=1; i<mulExps.size(); i++) {
            op = mulExps.get(i);
            ++i;
            right = mulExps.get(i);
            if (!left.startsWith("%") && !right.startsWith("%")) { // 双常量
                switch (op) {
                    case "+": {
                        left = Integer.parseInt(left) + Integer.parseInt(right) + "";
                        break;
                    }
                    case "-": {
                        left = Integer.parseInt(left) - Integer.parseInt(right) + "";
                        break;
                    }
                    default:;
                }
            }
            else {
                switch (op) {
                    case "+": {
                        System.out.print("%v" + regId + " = add i32 " + left + ", " + right + "\n");
                        break;
                    }
                    case "-": {
                        System.out.print("%v" + regId + " = sub i32 " + left + ", " + right + "\n");
                        break;
                    }
                    default:;
                }
                left = "%v" + regId;
                regId++;
            }
        }
        if (debug) System.out.print("AddExp returns " + left + "\n");
        Symbol ret = new Symbol("ret", left, SymClass.VAR, -1, !left.startsWith("%"));
        return ret;
    }
    
    private static Symbol ConstExp(GrammarTreeNode p) {
        GrammarTreeNode c = p.getChild();
        return AddExp(c);
    }
    
    private static Symbol Exp(GrammarTreeNode p) {
        GrammarTreeNode c = p.getChild();
        return AddExp(c);
    }
    
    
}

class Loop {
    private String condLabel;
    private String bodyLabel;
    private String outLabel;
    
    public Loop(String c, String b, String o) {
        condLabel = c;
        bodyLabel = b;
        outLabel = o;
    }
    
    public String getCondLabel() {
        return condLabel;
    }
    
    public void setCondLabel(String condLabel) {
        this.condLabel = condLabel;
    }
    
    public String getBodyLabel() {
        return bodyLabel;
    }
    
    public void setBodyLabel(String bodyLabel) {
        this.bodyLabel = bodyLabel;
    }
    
    public String getOutLabel() {
        return outLabel;
    }
    
    public void setOutLabel(String outLabel) {
        this.outLabel = outLabel;
    }
}