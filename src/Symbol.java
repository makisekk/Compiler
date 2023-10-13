import java.util.ArrayList;

public class Symbol {
    private String name;
    private String val = null;
    private ArrayList<String> valArray = null;
    private SymClass symClass;
    private int ln; // 行号
    private String regName = null;
    private boolean isConst = false;
    private int dimension = 0;   // 维数
    private int len1 = 1;   // 第一维长度
    private int len2 = 1;   // 第二维长度
    private int paramPos = 0;
    private boolean is1Bit = false;
    
    private String paramType = null;
    
    public ArrayList<String> getValArray() {
        return valArray;
    }
    
    public void setIs1Bit(boolean b) {
        is1Bit = b;
    }
    
    public void setRegName(String regName) {
        this.regName = regName;
    }
    
    public String getParamType() {
        return paramType;
    }
    
    public void setParamType(String paramType) {
        this.paramType = paramType;
    }
    
    public boolean getIs1Bit() {
        return is1Bit;
    }
    public void setVal(String s) {
        val = s;
    }
    
    public int getParamPos() {
        return paramPos;
    }
    
    public void setParamPos(int i) {
        paramPos = i;
    }
    
    public void setDimension(int i) {
        dimension = i;
    }
    
    public void setIsConst(boolean b) {
        isConst = b;
    }
    
    public int getLen2() {
        return len2;
    }
    
    public int getLen1() {
        return len1;
    }
    
    public String getName() {
        return name;
    }
    
    public String getRegName() {
        return regName;
    }
    
    public String getVal() {
        return val;
    }
    
    public SymClass getSymClass() {
        return symClass;
    }
    
    public int getLn() {
        return ln;
    }
    
    public int getDimension() {
        return dimension;
    }
    
    public Symbol(String errName, String val, int ln) {  // 错误处理填充符号,不含补位参数
        this.name = errName;
        this.val = val;
        this.symClass = SymClass.VAR;
        this.ln = ln;
    }
    
    public Symbol(String errName, int size, int ln) {  // 错误处理填充数组符号,不含补位参数
        this.name = errName;
        ArrayList<String> arr = new ArrayList<>();
        for (int i=0; i<size; i++) {
            arr.add("1");
        }
        this.valArray = arr;
        this.len1 = size;
        this.dimension = 1;
        this.symClass = SymClass.ARR1;
        this.ln = ln;
    }
    
    public Symbol(String name, String val, SymClass symClass, int ln) {  // 普通变量
        this.name = name;
        this.val = val;
        this.symClass = symClass;
        this.ln = ln;
    }
    
    public Symbol(String name, int size, ArrayList<String> vals, int ln, String regName) {  // 一维数组变量
        this.name = name;
        this.len1 = size;
        this.dimension = 1;
        this.valArray = vals;
        this.symClass = SymClass.ARR1;
        this.ln = ln;
        this.regName = regName;
    }
    
    public Symbol(String name, int size1, int size2, ArrayList<String> vals, int ln, String regName) {  // 二维数组变量
        this.name = name;
        this.len1 = size1;
        this.len2 = size2;
        this.dimension = 2;
        this.valArray = vals;
        this.symClass = SymClass.ARR2;
        this.ln = ln;
        this.regName = regName;
    }
    
    public Symbol(String name, String val, SymClass symClass, int ln, boolean isConst) {  // 普通const变量
        this.name = name;
        this.val = val;
        this.symClass = symClass;
        this.ln = ln;
        this.isConst = isConst;
    }
    
    public Symbol(String name, int size, ArrayList<String> vals, int ln, boolean isConst, String regName) {  // 一维const数组变量
        this.name = name;
        this.len1 = size;
        this.dimension = 1;
        this.valArray = vals;
        this.symClass = SymClass.ARR1;
        this.ln = ln;
        this.isConst = isConst;
        this.regName = regName;
    }
    
    public Symbol(String name, int size1, int size2, ArrayList<String> vals, int ln, boolean isConst, String regName) {  // 二维const数组变量
        this.name = name;
        this.len1 = size1;
        this.len2 = size2;
        this.dimension = 2;
        this.valArray = vals;
        this.symClass = SymClass.ARR2;
        this.ln = ln;
        this.isConst = isConst;
        this.regName = regName;
    }
    
    public Symbol(String name, SymClass symClass, int ln, String regName) {  // 普通变量 / 参数
        this.name = name;
        this.symClass = symClass;
        this.ln = ln;
        this.regName = regName;
    }
    
    public Symbol(String name, int dimension, int size2, SymClass symClass, int ln) {   // 数组参数
        this.name = name;
        this.dimension = dimension;
        this.len2 = size2;
        this.symClass = symClass;
        this.ln = ln;
        this.valArray = new ArrayList<>();
    }
    
    public Symbol(String name, String val, SymClass symClass, int ln, String regName) {  // 普通变量 / 参数
        this.name = name;
        this.val = val;
        this.symClass = symClass;
        this.ln = ln;
        this.regName = regName;
    }
    
    public boolean isConst() {
        return isConst ;//|| (!(symClass == SymClass.FILL) && (val != null && regName == null || valArray != null && !valArray.isEmpty() && regName == null));
    }
    
}

enum SymClass {
    PARA,
    ARR1,
    ARR2,
    VAR,
    ARR1_PARA,
    ARR2_PARA,
    FILL
}