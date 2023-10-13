public class FuncTable extends SymTable{
    private TokenType returnType;
    
    public FuncTable(TokenType returnType) {
        this.returnType = returnType;
    }
    
    public TokenType getReturnType() {
        return returnType;
    }
}
