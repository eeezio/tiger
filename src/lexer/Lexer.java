package lexer;

import static control.Control.ConLexer.dump;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;

import lexer.Token.Kind;
import slp.Main;
import util.Todo;

public class Lexer {
    String fname; // the input file name to be compiled
    InputStream fstream; // input stream for the above file
    Integer lineNum = 1;
    static HashMap<String, Token.Kind> lexerMap = new HashMap<>();
    boolean havePreLex = false;
    Token.Kind preLex;
    String preLexString;

    static {
        lexerMap.put("+", Kind.TOKEN_ADD);
        lexerMap.put("&&", Kind.TOKEN_AND);
        lexerMap.put("=", Kind.TOKEN_ASSIGN);
        lexerMap.put("boolean", Kind.TOKEN_BOOLEAN);
        lexerMap.put("class", Kind.TOKEN_CLASS);
        lexerMap.put(",", Kind.TOKEN_COMMER);
        lexerMap.put(".", Kind.TOKEN_DOT);
        lexerMap.put("else", Kind.TOKEN_ELSE);
        lexerMap.put("extends", Kind.TOKEN_EXTENDS);
        lexerMap.put("false", Kind.TOKEN_FALSE);
        //id 要特判
        lexerMap.put("if", Kind.TOKEN_IF);
        lexerMap.put("int", Kind.TOKEN_INT);
        lexerMap.put("{", Kind.TOKEN_LBRACE);
        lexerMap.put("[", Kind.TOKEN_LBRACK);
        lexerMap.put("length", Kind.TOKEN_LENGTH);
        lexerMap.put("(", Kind.TOKEN_LPAREN);
        lexerMap.put("<", Kind.TOKEN_LT);
        lexerMap.put("main", Kind.TOKEN_MAIN);
        lexerMap.put("new", Kind.TOKEN_NEW);
        lexerMap.put("!", Kind.TOKEN_NOT);
        //num 也要特判
        lexerMap.put("out", Kind.TOKEN_OUT);
        lexerMap.put("println", Kind.TOKEN_PRINTLN);
        lexerMap.put("public", Kind.TOKEN_PUBLIC);
        lexerMap.put("}", Kind.TOKEN_RBRACE);
        lexerMap.put("]", Kind.TOKEN_RBRACK);
        lexerMap.put("return", Kind.TOKEN_RETURN);
        lexerMap.put(")", Kind.TOKEN_RPAREN);
        lexerMap.put(";", Kind.TOKEN_SEMI);
        lexerMap.put("static", Kind.TOKEN_STATIC);
        lexerMap.put("String", Kind.TOKEN_STRING);
        lexerMap.put("-", Kind.TOKEN_SUB);
        lexerMap.put("System", Kind.TOKEN_SYSTEM);
        lexerMap.put("this", Kind.TOKEN_THIS);
        lexerMap.put("*", Kind.TOKEN_TIMES);
        lexerMap.put("true", Kind.TOKEN_TRUE);
        lexerMap.put("void", Kind.TOKEN_VOID);
        lexerMap.put("while", Kind.TOKEN_WHILE);
    }

    public Lexer(String fname, InputStream fstream) {
        this.fname = fname;
        this.fstream = fstream;
    }

    // When called, return the next token (refer to the code "Token.java")
    // from the input stream.
    // Return TOKEN_EOF when reaching the end of the input stream.
    private Token nextTokenInternal() throws Exception {
        if (this.havePreLex) {
            this.havePreLex = false;
            return new Token(this.preLex, lineNum, preLexString);
        }
        int c = this.fstream.read();
        if (-1 == c)
            // The value for "lineNum" is now "null",
            // you should modify this to an appropriate
            // line number for the "EOF" token.
            return new Token(Kind.TOKEN_EOF, lineNum);

        // skip all kinds of "blanks"
        while (' ' == c || '\t' == c || '\n' == c || '\r' == c) {
            if (c == '\n') {
                lineNum++;
            }
            c = this.fstream.read();
        }
        if (-1 == c)
            return new Token(Kind.TOKEN_EOF, lineNum);
        if (lexerMap.containsKey(Character.toString(c))) {
            return new Token(lexerMap.get(Character.toString(c)), lineNum, Character.toString(c));
        }
        switch (c) {
            case '+':
                return new Token(Kind.TOKEN_ADD, lineNum, "+");
            default:
                // Lab 1, exercise 2: supply missing code to
                // lex other kinds of tokens.
                // Hint: think carefully about the basic
                // data structure and algorithms. The code
                // is not that much and may be less than 50 lines. If you
                // find you are writing a lot of code, you
                // are on the wrong way.
//                new Todo();
                String tmp = Character.toString(c);
                boolean id = false;
                if (c < '0' || c > '9') {
                    id = true;
                }

                while (true) {
                    c = this.fstream.read();
                    if (' ' == c || '\t' == c || '\n' == c || '\r' == c) {
                        break;
                    }
                    if (lexerMap.containsKey(Character.toString(c))) {
                        break;
                    }
                    if (c < '0' || c > '9') {
                        id = true;
                    }
                    tmp += Character.toString(c);
                    if (tmp.equals("//")) {
                        c = this.fstream.read();
                        while (c != '\n') {
                            c = this.fstream.read();
                        }
                        lineNum++;
                        return nextTokenInternal();
                    }
                }
                if (c == '\n') {
                    lineNum++;
                }
                if (lexerMap.containsKey(Character.toString(c))) {
                    this.havePreLex = true;
                    this.preLexString = Character.toString(c);
                    this.preLex = lexerMap.get(preLexString);
                }
                if (lexerMap.containsKey(tmp)) {
                    return new Token(lexerMap.get(tmp), lineNum, tmp);
                } else {
                    if (id) {
                        return new Token(Kind.TOKEN_ID, lineNum, tmp);
                    } else {
                        return new Token(Kind.TOKEN_NUM, lineNum, tmp);
                    }
                }
        }
    }

    public Token nextToken() {
        Token t = null;

        try {
            t = this.nextTokenInternal();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (dump)
            System.out.println(t.toString());
        return t;
    }
}
