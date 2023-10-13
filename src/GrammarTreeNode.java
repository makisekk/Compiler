public class GrammarTreeNode {
    private GrammarTreeNode parent = null;
    private GrammarTreeNode child = null;
    private GrammarTreeNode next = null;
    private Token token = null;
    
    public GrammarTreeNode(Token token) {
        this.token = token;
    }
    
    public GrammarTreeNode getParent() {
        return parent;
    }
    
    public void setParent(GrammarTreeNode parent) {
        this.parent = parent;
    }
    
    public GrammarTreeNode getChild() {
        return child;
    }
    
    public GrammarTreeNode getChild(int i) {
        GrammarTreeNode c = child;
        if (c == null) return null;
        while (i > 0) {
            c = c.getNext();
            if (c == null) return null;
            i--;
        }
        return c;
    }
    
    public void addChild(GrammarTreeNode child) {
        if (this.getChild() == null) {
            this.child = child;
        }
        else {
            GrammarTreeNode son = this.getChild();
            while (son.getNext() != null) son = son.getNext();
            son.next = child;
        }
    }
    
    public GrammarTreeNode getNext() {
        return next;
    }
    
    public Token getToken() {
        return token;
    }
    
    public void setToken(Token token) {
        this.token = token;
    }
    
    public TokenType getType() { return token.getType(); }
    
    public String getVal() { return token.getVal(); }
    
    public TokenType getNextType() {
        if (next != null) return next.getType();
        else return null;
    }
    
    public String getNextVal() {
        if (next != null) return next.getVal();
        else return null;
    }
    
    public TokenType getChildType(int i) {
        GrammarTreeNode c = child;
        if (c == null) return null;
        while (i > 0) {
            c = c.getNext();
            if (c == null) return null;
            i--;
        }
        return c.getType();
    }
    
    public int getChildsSize() {
        int r;
        if (child == null) return 0;
        else {
            r = 1;
            GrammarTreeNode c = child;
            while ((c = c.getNext()) != null) ++r;
            return r;
        }
    }
    
}
