package parser;

import ast.Ast;
import lexer.Lexer;
import lexer.Token;
import lexer.Token.Kind;

import java.util.LinkedList;

public class Parser {
    Lexer lexer;
    Token current;
    String last_step;//用于处理statement与exp二义性的临时id保存

    public Parser(String fname, java.io.InputStream fstream) {
        lexer = new Lexer(fname, fstream);
        current = lexer.nextToken();
    }

    // /////////////////////////////////////////////
    // utility methods to connect the lexer
    // and the parser.

    private void advance() {
        current = lexer.nextToken();
    }

    private void eatToken(Kind kind) {
        if (kind == current.kind)
            advance();
        else {
            System.out.println("Expects: " + kind.toString());
            System.out.println("But got: " + current.kind.toString());
            System.out.println("at line " + current.lineNum);
            System.out.println("error lexme is " + current.lexeme);
            System.exit(1);
        }
    }

    private void error() {
        System.out.println("Syntax error: compilation aborting...\n");
        System.out.println("at line " + current.lineNum);
        System.out.println("error lexme is " + current.lexeme);
        System.exit(1);
        return;
    }

    // ////////////////////////////////////////////////////////////
    // below are method for parsing.

    // A bunch of parsing methods to parse expressions. The messy
    // parts are to deal with precedence and associativity.

    // ExpList -> Exp ExpRest*
    // ->
    // ExpRest -> , Exp
    private LinkedList<Ast.Exp.T> parseExpList() {
        if (current.kind == Kind.TOKEN_RPAREN)
            return null;
        LinkedList<Ast.Exp.T> ans = new LinkedList<>();
        ans.add(parseExp());
        while (current.kind == Kind.TOKEN_COMMER) {
            advance();
            ans.add(parseExp());
        }
        return ans;
    }

    // AtomExp -> (exp)
    // -> INTEGER_LITERAL
    // -> true
    // -> false
    // -> this
    // -> id
    // -> new int [exp]
    // -> new id ()
    private Ast.Exp.T parseAtomExp() {
        switch (current.kind) {
            case TOKEN_LPAREN:
                advance();
                Ast.Exp.T ans = parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                return ans;
            case TOKEN_NUM:
                int num = Integer.parseInt(current.lexeme);
                advance();
                return new Ast.Exp.Num(num);
            case TOKEN_TRUE:
                advance();
                return new Ast.Exp.True();
            case TOKEN_FALSE:
                advance();
                return new Ast.Exp.False();
            case TOKEN_THIS:
                advance();
                return new Ast.Exp.This();
            case TOKEN_ID:
                String id = current.lexeme;
                advance();
                return new Ast.Exp.Id(id);
            case TOKEN_NEW: {
                advance();
                switch (current.kind) {
                    case TOKEN_INT:
                        advance();
                        eatToken(Kind.TOKEN_LBRACK);
                        Ast.Exp.T exp = parseExp();
                        eatToken(Kind.TOKEN_RBRACK);
                        return new Ast.Exp.NewIntArray(exp);
                    case TOKEN_ID:
                        String newId = current.lexeme;
                        advance();
                        eatToken(Kind.TOKEN_LPAREN);
                        eatToken(Kind.TOKEN_RPAREN);
                        return new Ast.Exp.NewObject(newId);
                    default:
                        error();
                        return null;
                }
            }
            default:
                error();
                return null;
        }
    }

    // NotExp -> AtomExp
    // -> AtomExp .id (expList)
    // -> AtomExp [exp]
    // -> AtomExp .length
    private Ast.Exp.T parseNotExp() {
        Ast.Exp.T exp = parseAtomExp();
        while (current.kind == Kind.TOKEN_DOT || current.kind == Kind.TOKEN_LBRACK) {
            if (current.kind == Kind.TOKEN_DOT) {
                advance();
                if (current.kind == Kind.TOKEN_LENGTH) {
                    advance();
                    return new Ast.Exp.Not(new Ast.Exp.Length(exp));
                }
                String id = current.lexeme;
                eatToken(Kind.TOKEN_ID);
                eatToken(Kind.TOKEN_LPAREN);
                LinkedList<Ast.Exp.T> args = parseExpList();
                eatToken(Kind.TOKEN_RPAREN);
                return new Ast.Exp.Call(exp, id, args);
            } else {
                advance();
                Ast.Exp.T index = parseExp();
                eatToken(Kind.TOKEN_RBRACK);
                return new Ast.Exp.Not(new Ast.Exp.ArraySelect(exp, index));
            }
        }
        return exp;
    }

    // TimesExp -> ! TimesExp
    // -> NotExp
    private Ast.Exp.T parseTimesExp() {
        while (current.kind == Kind.TOKEN_NOT) {
            advance();
        }
        return parseNotExp();
    }

    // AddSubExp -> TimesExp * TimesExp
    // -> TimesExp
    private Ast.Exp.T parseAddSubExp() {
        Ast.Exp.T ans = parseTimesExp();
        while (current.kind == Kind.TOKEN_TIMES) {
            advance();
            ans = new Ast.Exp.Times(ans, parseTimesExp());
        }
        return ans;
    }

    // LtExp -> AddSubExp + AddSubExp
    // -> AddSubExp - AddSubExp
    // -> AddSubExp
    private Ast.Exp.T parseLtExp() {
        Ast.Exp.T ans = parseAddSubExp();
        while (current.kind == Kind.TOKEN_ADD || current.kind == Kind.TOKEN_SUB) {
            boolean add = true;
            if (current.kind == Kind.TOKEN_SUB) {
                add = false;
            }
            advance();
            if (add) {
                ans = new Ast.Exp.Add(ans, parseAddSubExp());
            } else {
                ans = new Ast.Exp.Sub(ans, parseAddSubExp());
            }
        }
        return ans;
    }

    // AndExp -> LtExp < LtExp
    // -> LtExp
    private Ast.Exp.T parseAndExp() {
        Ast.Exp.T ans = parseLtExp();
        while (current.kind == Kind.TOKEN_LT) {
            advance();
            ans = new Ast.Exp.Lt(ans, parseLtExp());
        }
        return ans;
    }

    // Exp -> AndExp && AndExp
    // -> AndExp
    private Ast.Exp.T parseExp() {
        Ast.Exp.T ans = parseAndExp();
        while (current.kind == Kind.TOKEN_AND) {
            advance();
            ans = new Ast.Exp.And(ans, parseAndExp());
        }
        return ans;
    }

    // Statement -> { Statement* }
    // -> if ( Exp ) Statement else Statement
    // -> while ( Exp ) Statement
    // -> System.out.println ( Exp ) ;
    // -> id = Exp ;
    // -> id [ Exp ]= Exp ;
    private Ast.Stm.T parseStatement() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a statement.
//    new util.Todo();
        switch (current.kind) {
            case TOKEN_LBRACE:
                advance();
                LinkedList<Ast.Stm.T> stms = parseStatements();
                eatToken(Kind.TOKEN_RBRACE);
                return new Ast.Stm.Block(stms);
            case TOKEN_IF:
                advance();
                eatToken(Kind.TOKEN_LPAREN);
                Ast.Exp.T conditon = parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                Ast.Stm.T thenn = parseStatement();
                eatToken(Kind.TOKEN_ELSE);
                Ast.Stm.T elsee = parseStatement();
                return new Ast.Stm.If(conditon, thenn, elsee);
            case TOKEN_WHILE:
                advance();
                eatToken(Kind.TOKEN_LPAREN);
                Ast.Exp.T condition = parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                Ast.Stm.T stm = parseStatement();
                return new Ast.Stm.While(condition, stm);
            case TOKEN_SYSTEM:
                advance();
                eatToken(Kind.TOKEN_DOT);
                eatToken(Kind.TOKEN_OUT);
                eatToken(Kind.TOKEN_DOT);
                eatToken(Kind.TOKEN_PRINTLN);
                eatToken(Kind.TOKEN_LPAREN);
                Ast.Exp.T exp = parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                eatToken(Kind.TOKEN_SEMI);
                return new Ast.Stm.Print(exp);
            case TOKEN_ID:
                String id = current.lexeme;
                advance();
                if (current.kind == Kind.TOKEN_ASSIGN) {
                    advance();
                    Ast.Exp.T assexp = parseExp();
                    eatToken(Kind.TOKEN_SEMI);
                    return new Ast.Stm.Assign(id, assexp);
                } else if (current.kind == Kind.TOKEN_LBRACK) {
                    advance();
                    Ast.Exp.T index = parseExp();
                    eatToken(Kind.TOKEN_RBRACK);
                    eatToken(Kind.TOKEN_ASSIGN);
                    Ast.Exp.T assexp = parseExp();
                    eatToken(Kind.TOKEN_SEMI);
                    return new Ast.Stm.AssignArray(id, index, assexp);
                }
                return null;
            default:
                error();
        }
        return null;
    }

    // Statements -> Statement Statements
    // ->
    private LinkedList<Ast.Stm.T> parseStatements() {
        LinkedList<Ast.Stm.T> ans = new LinkedList<>();
        while (current.kind == Kind.TOKEN_LBRACE || current.kind == Kind.TOKEN_IF
                || current.kind == Kind.TOKEN_WHILE
                || current.kind == Kind.TOKEN_SYSTEM || current.kind == Kind.TOKEN_ID) {
            ans.add(parseStatement());
        }
        return ans;
    }

    // Type -> int []
    // -> boolean
    // -> int
    // -> id
    private Ast.Type.T parseType() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a type.
//    new util.Todo();
        switch (current.kind) {
            case TOKEN_INT:
                advance();
                if (current.kind == Kind.TOKEN_LBRACK) {
                    eatToken(Kind.TOKEN_LBRACK);
                    eatToken(Kind.TOKEN_RBRACK);
                    return new Ast.Type.IntArray();
                }
                return new Ast.Type.Int();
            case TOKEN_BOOLEAN:
                advance();
                return new Ast.Type.Boolean();
            case TOKEN_ID:
                String id = current.lexeme;
                advance();
                return new Ast.Type.ClassType(id);
            default:
                error();
        }
        return null;
    }

    private boolean testEatToken(Kind kind) {
        if (kind == current.kind) {
            advance();
            return true;
        } else {
            return false;
        }
    }

    // VarDecl -> Type id ;
    private Ast.Dec.T parseVarDecl() {
        // to parse the "Type" nonterminal in this method, instead of writing
        // a fresh one.
        Ast.Type.T type = parseType();
//        eatToken(Kind.TOKEN_ID);
//        eatToken(Kind.TOKEN_SEMI);
        String id = current.lexeme;
        if (testEatToken(Kind.TOKEN_ID)) {
            eatToken(Kind.TOKEN_SEMI);
            return new Ast.Dec.DecSingle(type, id);
        }
        return null;
    }

    // VarDecls -> VarDecl VarDecls
    // ->
    private LinkedList<Ast.Dec.T> parseVarDecls() {
        LinkedList<Ast.Dec.T> ans = new LinkedList<>();
        while (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
                || current.kind == Kind.TOKEN_ID) {
            last_step = current.lexeme;
            Ast.Dec.T tmp = parseVarDecl();
            if (tmp != null) {
                ans.add(tmp);
            }
        }
        return ans;
    }

    // FormalList -> Type id FormalRest*
    // ->
    // FormalRest -> , Type id
    private LinkedList<Ast.Dec.T> parseFormalList() {
        LinkedList<Ast.Dec.T> ans = new LinkedList<>();
        if (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
                || current.kind == Kind.TOKEN_ID) {
            Ast.Type.T type = parseType();
            String id = current.lexeme;
            eatToken(Kind.TOKEN_ID);
            ans.add(new Ast.Dec.DecSingle(type, id));
            while (current.kind == Kind.TOKEN_COMMER) {
                advance();
                type = parseType();
                id = current.lexeme;
                eatToken(Kind.TOKEN_ID);
                ans.add(new Ast.Dec.DecSingle(type, id));
            }
        }
        return ans;
    }

    // Method -> public Type id ( FormalList )
    // { VarDecl* Statement* return Exp ;}
    private Ast.Method.T parseMethod() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a method.
//        new util.Todo();
        if (current.kind == Kind.TOKEN_PUBLIC) {
            eatToken(Kind.TOKEN_PUBLIC);
            Ast.Type.T type = parseType();
            String id = current.lexeme;
            eatToken(Kind.TOKEN_ID);
            eatToken(Kind.TOKEN_LPAREN);
            LinkedList<Ast.Dec.T> formals = parseFormalList();
            eatToken(Kind.TOKEN_RPAREN);
            eatToken(Kind.TOKEN_LBRACE);
            LinkedList<Ast.Dec.T> locals = parseVarDecls();
            Ast.Stm.T tmp = null;
            if (current.kind == Kind.TOKEN_ASSIGN) {
                advance();
                tmp = new Ast.Stm.Assign(last_step, parseExp());
                last_step = null;
                eatToken(Kind.TOKEN_SEMI);
            }
            LinkedList<Ast.Stm.T> stms = parseStatements();
            if (tmp != null) {
                stms.add(0, tmp);
            }
            eatToken(Kind.TOKEN_RETURN);
            Ast.Exp.T retExp = parseExp();
            eatToken(Kind.TOKEN_SEMI);
            eatToken(Kind.TOKEN_RBRACE);
            return new Ast.Method.MethodSingle(type, id, formals, locals, stms, retExp);
        } else {
            error();
        }
        return null;
    }

    // MethodDecls -> MethodDecl MethodDecls
    // ->
    private LinkedList<Ast.Method.T> parseMethodDecls() {
        LinkedList<Ast.Method.T> ans = new LinkedList<>();
        while (current.kind == Kind.TOKEN_PUBLIC) {
            ans.add(parseMethod());
        }
        return ans;
    }

    // ClassDecl -> class id { VarDecl* MethodDecl* }
    // -> class id extends id { VarDecl* MethodDecl* }
    private Ast.Class.T parseClassDecl() {
        eatToken(Kind.TOKEN_CLASS);
        String id = current.lexeme;
        eatToken(Kind.TOKEN_ID);
        String extendss = null;
        if (current.kind == Kind.TOKEN_EXTENDS) {
            extendss = current.lexeme;
            eatToken(Kind.TOKEN_EXTENDS);
            eatToken(Kind.TOKEN_ID);
        }
        eatToken(Kind.TOKEN_LBRACE);
        LinkedList<Ast.Dec.T> decs = parseVarDecls();
        LinkedList<Ast.Method.T> methods = parseMethodDecls();
        eatToken(Kind.TOKEN_RBRACE);
        return new Ast.Class.ClassSingle(id, extendss, decs, methods);
    }

    // ClassDecls -> ClassDecl ClassDecls
    // ->
    private LinkedList<Ast.Class.T> parseClassDecls() {
        LinkedList<Ast.Class.T> ans = new LinkedList<>();
        while (current.kind == Kind.TOKEN_CLASS) {
            ans.add(parseClassDecl());
        }
        return ans;
    }

    // MainClass -> class id
    // {
    // public static void main ( String [] id )
    // {
    // Statement
    // }
    // }
    private Ast.MainClass.MainClassSingle parseMainClass() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a main class as described by the
        // grammar above.
//        new util.Todo();
        eatToken(Kind.TOKEN_CLASS);
        String id = current.lexeme;
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_LBRACE);
        eatToken(Kind.TOKEN_PUBLIC);
        eatToken(Kind.TOKEN_STATIC);
        eatToken(Kind.TOKEN_VOID);
        eatToken(Kind.TOKEN_MAIN);
        eatToken(Kind.TOKEN_LPAREN);
        String args = current.lexeme;
        eatToken(Kind.TOKEN_STRING);
        eatToken(Kind.TOKEN_LBRACK);
        eatToken(Kind.TOKEN_RBRACK);
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_RPAREN);
        eatToken(Kind.TOKEN_LBRACE);
        Ast.Stm.T stm = parseStatement();
        eatToken(Kind.TOKEN_RBRACE);
        eatToken(Kind.TOKEN_RBRACE);
        return new Ast.MainClass.MainClassSingle(id, args, stm);
    }

    // Program -> MainClass ClassDecl*
    private Ast.Program.T parseProgram() {
        Ast.MainClass.T mainClass = parseMainClass();
        LinkedList<Ast.Class.T> classes = parseClassDecls();
        eatToken(Kind.TOKEN_EOF);
        System.out.println("program parse over");
        return new Ast.Program.ProgramSingle(mainClass, classes);
//    return;
    }

    public ast.Ast.Program.T parse() {
        return parseProgram();
//    return null;
    }
}
