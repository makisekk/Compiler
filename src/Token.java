public class Token {
    private TokenType type;
    private String val;
    private int ln;             //行号
    
    public TokenType getType() {
        return type;
    }
    
    public String getVal() {
        return val;
    }
    
    public int getLn() {
        return ln;
    }
    
    public Token(TokenType type, String val, Integer ln) {
        this.type = type;
        this.val = val;
        this.ln = ln == null ? -1 : ln;
        
        
    }
}

enum TokenType {
    IDENFR,
    INTCON,
    STRCON,
    MAINTK,
    CONSTTK,
    INTTK,
    BREAKTK,
    CONTINUETK,
    IFTK,
    ELSETK,
    NOT,
    AND,
    OR,
    WHILETK,
    GETINTTK,
    PRINTFTK,
    RETURNTK,
    PLUS,
    MINU,
    VOIDTK,
    MULT,
    DIV,
    MOD,
    LSS,
    LEQ,
    GRE,
    GEQ,
    EQL,
    NEQ,
    ASSIGN,
    SEMICN,
    COMMA,
    LPARENT,
    RPARENT,
    LBRACK,
    RBRACK,
    LBRACE,
    RBRACE,
    CompUnit,
    Decl,
    FuncDef,
    MainFuncDef,
    ConstDecl,
    BType,
    ConstDef,
    ConstInitVal,
    VarDecl,
    VarDef,
    InitVal,
    FuncType,
    FuncFParams,
    FuncFParam,
    Block,
    BlockItem,
    Stmt,
    Exp,
    Cond,
    LVal,
    PrimaryExp,
    Number,
    UnaryExp,
    UnaryOp,
    FuncRParams,
    MulExp,
    AddExp,
    RelExp,
    EqExp,
    LAndExp,
    LOrExp,
    ConstExp
}