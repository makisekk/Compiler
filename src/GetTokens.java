import java.io.File;
import java.util.ArrayList;
import java.io.Reader;
import java.io.FileReader;
import java.util.HashMap;

//词法分析程序
public class GetTokens {
    private static TokenType isKeyword(String s) {
        HashMap<String, TokenType> keywords = new HashMap<>();  // 关键字表
        keywords.put("break",TokenType.BREAKTK);
        keywords.put("const",TokenType.CONSTTK);
        keywords.put("continue",TokenType.CONTINUETK);
        keywords.put("else",TokenType.ELSETK);
        keywords.put("getint",TokenType.GETINTTK);
        keywords.put("if",TokenType.IFTK);
        keywords.put("int",TokenType.INTTK);
        keywords.put("main",TokenType.MAINTK);
        keywords.put("printf",TokenType.PRINTFTK);
        keywords.put("return",TokenType.RETURNTK);
        keywords.put("void",TokenType.VOIDTK);
        keywords.put("while",TokenType.WHILETK);
        return keywords.get(s);
    }
    
    public static void run(ArrayList<Token> tokens) throws Exception{
        File inFile = new File("./testfile.txt");
        Reader rd = new FileReader(inFile);
        int ln = 1;         // 行号
        int ch = rd.read();
        while (ch != -1) {
            if (ch == ' ' || ch == '\t') {
                ch = rd.read();
                continue;
            }
            
            if (Character.isJavaIdentifierStart(ch)) {  // ident
                String ident = Character.toString((char)ch);
                while ((ch = rd.read()) != -1 && Character.isJavaIdentifierPart(ch)) {
                    ident +=  Character.toString((char)ch);;
                }
                TokenType tt;
                if ((tt = isKeyword(ident)) != null) {
                    Token t = new Token(tt, ident, ln);
                    tokens.add(t);
                }
                else {
                    Token t = new Token(TokenType.IDENFR, ident, ln);
                    tokens.add(t);
                }
                continue;
            }
            
            if (Character.isDigit(ch)) {                // int
                String num =  Character.toString((char)ch);
                while ((ch = rd.read()) != -1 && Character.isDigit(ch)) {
                    num += Character.toString((char)ch);
                }
                Token t = new Token(TokenType.INTCON, num, ln);
                tokens.add(t);
                continue;
            }
            
            if (ch == '\"') {
                String formatStr = Character.toString((char)ch);
                while ((ch = rd.read()) != -1 && ch != '\"') {
                    formatStr += Character.toString((char)ch);
                }
                formatStr += Character.toString((char)ch);
                ch = rd.read();
                Token t = new Token(TokenType.STRCON, formatStr, ln);
                tokens.add(t);
                continue;
            }
            
            if (ch == '/') {
                if ((ch = rd.read()) == '/') {                    // 单行注释, 快进到下一行
                    while ((ch = rd.read()) != -1 && ch != '\n') { }
                    if (ch != -1) {
                        ch = rd.read();
                        ln++;
                    }
                    continue;
                }
                else if (ch == '*') {                   // 多行注释， 快进到注释结束
                    while ((ch = rd.read()) != -1) {
                        if (ch == '\n') {
                            ln++;
                            continue;
                        }
                        if (ch == '*') {
                            while ((ch = rd.read()) == '*') {}
                            if (ch == -1) break;
                            if (ch == '/') {
                                ch = rd.read();
                                break;
                            }
                        }
                    }
                }
                else {
                    Token t = new Token(TokenType.DIV, "/", ln);
                    tokens.add(t);
                }
            }
            
            if (ch == '+') {
                Token t = new Token(TokenType.PLUS, "+", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == '-') {
                Token t = new Token(TokenType.MINU, "-", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == '*') {
                Token t = new Token(TokenType.MULT, "*", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == '%') {
                Token t = new Token(TokenType.MOD, "%", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == ',') {
                Token t = new Token(TokenType.COMMA, ",", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == ';') {
                Token t = new Token(TokenType.SEMICN, ";", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == '(') {
                Token t = new Token(TokenType.LPARENT, "(", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == ')') {
                Token t = new Token(TokenType.RPARENT, ")", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == '[') {
                Token t = new Token(TokenType.LBRACK, "[", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == ']') {
                Token t = new Token(TokenType.RBRACK, "]", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == '{') {
                Token t = new Token(TokenType.LBRACE, "{", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == '}') {
                Token t = new Token(TokenType.RBRACE, "}", ln);
                tokens.add(t);
                ch = rd.read();
                continue;
            }
            if (ch == '&') {
                if ((ch = rd.read()) == '&') {
                    Token t = new Token(TokenType.AND, "&&", ln);
                    tokens.add(t);
                    ch = rd.read();
                    continue;
                }
                else {
                    System.out.println("error only 1 '&' in line " + ln);
                    continue;
                }
            }
            if (ch == '|') {
                if ((ch = rd.read()) == '|') {
                    Token t = new Token(TokenType.OR, "||", ln);
                    tokens.add(t);
                    ch = rd.read();
                    continue;
                }
                else {
                    System.out.println("error only 1 '|' in line " + ln);
                    continue;
                }
            }
            if (ch == '>') {
                if ((ch = rd.read()) == '=') {
                    Token t = new Token(TokenType.GEQ, ">=", ln);
                    tokens.add(t);
                    ch = rd.read();
                    continue;
                }
                else {
                    Token t = new Token(TokenType.GRE, ">", ln);
                    tokens.add(t);
                    continue;
                }
            }
            if (ch == '<') {
                if ((ch = rd.read()) == '=') {
                    Token t = new Token(TokenType.LEQ, "<=", ln);
                    tokens.add(t);
                    ch = rd.read();
                    continue;
                }
                else {
                    Token t = new Token(TokenType.LSS, "<", ln);
                    tokens.add(t);
                    continue;
                }
            }
            if (ch == '=') {
                if ((ch = rd.read()) == '=') {
                    Token t = new Token(TokenType.EQL, "==", ln);
                    tokens.add(t);
                    ch = rd.read();
                    continue;
                }
                else {
                    Token t = new Token(TokenType.ASSIGN, "=", ln);
                    tokens.add(t);
                    continue;
                }
            }
            if (ch == '!') {
                if ((ch = rd.read()) == '=') {
                    Token t = new Token(TokenType.NEQ, "!=", ln);
                    tokens.add(t);
                    ch = rd.read();
                    continue;
                }
                else {
                    Token t = new Token(TokenType.NOT, "!", ln);
                    tokens.add(t);
                    continue;
                }
            }
            if (ch == '\r' || ch == '\n') {
                if (ch == '\r') ch = rd.read();
                ch = rd.read();
                ln++;
                continue;
            }
        }
        rd.close();
    }
}
