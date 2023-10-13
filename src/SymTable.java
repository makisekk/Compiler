import java.util.ArrayList;

public class SymTable {
    
    private final ArrayList<Symbol> symbols = new ArrayList<>();
    private SymTable parent = null;
    private int paramCnt = 0;
    
    public SymTable() {
    }
    
    public SymTable goOut() {
        return parent;
    }
    
    public void setParent(SymTable p) {
        parent = p;
    }
    
    public void putSym(Symbol s) {
        if (s.getSymClass() == SymClass.PARA || s.getSymClass() == SymClass.ARR1_PARA || s.getSymClass() == SymClass.ARR2_PARA) {
            s.setParamPos(paramCnt);
            paramCnt++;
        }
        if (symbols.isEmpty()) {
            symbols.add(s);
            return;
        }
        int i = 0;
        while (i < symbols.size() && symbols.get(i).getName().compareTo(s.getName()) < 0) ++i;
        symbols.add(i, s);
    }
    
    public Symbol findHere(String name) {
        if (symbols.isEmpty()) {
            return null;
        }
        int l = 0, r = symbols.size() - 1, m;
        while (l <= r) {
            m = (l + r) / 2;
            if (symbols.get(m).getName().compareTo(name) < 0) {
                l = m + 1;
            }
            else if (symbols.get(m).getName().compareTo(name) > 0) {
                r = m - 1;
            }
            else return symbols.get(m);
        }
        return null;
    }
    
    public Symbol findName(String name) {     // 返回该符号最近的定义
        SymTable cur = this;
        while (cur != null) {
            if (cur.findHere(name) != null) return cur.findHere(name);
            cur = cur.goOut();
        }
        return null;
    }
    
    public ArrayList<Symbol> getParams() {  // 按参数定义顺序输出参数
        ArrayList<Symbol> params = new ArrayList<>();
        for (int i=0; i<paramCnt; i++) {
            params.add(null);
        }
        for (Symbol s :symbols) {
            if (s.getSymClass() == SymClass.PARA || s.getSymClass() == SymClass.ARR1_PARA || s.getSymClass() == SymClass.ARR2_PARA) {
                params.set(s.getParamPos(),s);
            }
        }
        return params;
    }
    
    public int length() {
        return symbols.size();
    }
    
}
