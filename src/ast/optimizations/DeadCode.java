package ast.optimizations;

import ast.Ast;
import ast.Ast.Class.ClassSingle;
import ast.Ast.Dec.DecSingle;
import ast.Ast.MainClass.MainClassSingle;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Program.ProgramSingle;
import ast.Ast.Type.Boolean;
import ast.Ast.Type.ClassType;
import ast.Ast.Type.Int;
import ast.Ast.Type.IntArray;
import ast.Ast.Exp.*;
import ast.Ast.Stm.*;
import ast.Ast.Type.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

// Dead code elimination optimizations on an AST.

public class DeadCode implements ast.Visitor {
    private LinkedList<Ast.Class.T> newClasss;
    private ast.Ast.MainClass.T mainClass;
    public ast.Ast.Program.T program;

    //用于删除死声明
    private HashMap<String, Ast.Dec.DecSingle> curUsedClassDecs;
    private HashMap<String, Ast.Dec.DecSingle> curUsedMethodDecs;
    private HashMap<String, Ast.Dec.DecSingle> curDecClassDecs;
    private HashMap<String, Ast.Dec.DecSingle> curDecMethodDecs;

    boolean expModify;
    boolean stmModify;
    boolean globalOpt = true;
    int expAns;//当前能得出的表达式真值,-1假，0未知，1真。

    private LinkedList<Ast.Method.T> curMethods;//新AST中的新ClassSinge节点中的method filed，此参数作为构造函数参数.
    private Ast.Exp.T curChangeExp;
    private LinkedList<Ast.Stm.T> curStms;//构造出新AST时，对于新Method节点中的Stms field，该参数作为构造函数的参数。
    private Ast.Stm.T curChangeStm;//此变量用于append到curStm中

    public DeadCode() {
        this.newClasss = new LinkedList<>();
        this.mainClass = null;
        this.program = null;
        curStms = null;
        curUsedClassDecs = null;
        curUsedMethodDecs = null;
        curDecMethodDecs = null;
        curDecClassDecs = null;
        curChangeStm = null;
        expModify = false;
        expAns = 0;
    }

    private void putDecsInLinkedListToMap(LinkedList<Ast.Dec.T> decs, HashMap<String, Ast.Dec.DecSingle> map) {
        for (Ast.Dec.T dec : decs
        ) {
            map.put(((DecSingle) dec).id, (DecSingle) dec);
        }
    }

    private void putDecsInMapToLinkedList(LinkedList<Ast.Dec.T> linkedList, HashMap<String, Ast.Dec.DecSingle> decs) {
        for (String dec : decs.keySet()
        ) {
            linkedList.add(decs.get(dec));
        }
    }

    private void checkDec(String objName) {
        if (curDecMethodDecs.containsKey(objName))
            curUsedMethodDecs.put(objName, curDecMethodDecs.get(objName));
        else if (curDecClassDecs.containsKey(objName))
            curUsedClassDecs.put(objName, curDecClassDecs.get(objName));
    }

    private void binaryConstFoldOpt(Ast.Exp.BinaryExp e) {
        boolean expModifyBk = expModify;
        expModify = false;
        e.left.accept(this);
        Ast.Exp.T left = curChangeExp;
        e.right.accept(this);
        Ast.Exp.T right = curChangeExp;
        if (right instanceof Num && left instanceof Num) {
            int ans = 0;
            if (e instanceof Sub)
                ans = ((Num) right).num - ((Num) left).num;
            else if (e instanceof Add)
                ans = ((Num) right).num + ((Num) left).num;
            else
                ans = ((Num) right).num * ((Num) left).num;
            expModify = true;
            globalOpt = true;
            curChangeExp = new Num(ans);
        } else {
            if (e.left instanceof Id) {
                checkDec(((Id) e.left).id);
            }
            if (e.right instanceof Id) {
                checkDec(((Id) e.right).id);
            }
            if (e.left instanceof Num && ((Num) e.left).num == 0) {
                curChangeExp = new Num(0);
                globalOpt = true;
                expModify = true;
                return;
            }
            if (e.right instanceof Num && ((Num) e.right).num == 0) {
                curChangeExp = new Num(0);
                globalOpt = true;
                expModify = true;
                return;
            }
            if (expModify) {
                if (e instanceof Times)
                    curChangeExp = new Times(left, right);
                else if (e instanceof Sub)
                    curChangeExp = new Sub(left, right);
                else
                    curChangeExp = new Add(left, right);
            } else {
                curChangeExp = e;
                expModify = expModifyBk;
            }
        }
        return;
    }

    // //////////////////////////////////////////////////////
    //
    public String genId() {
        return util.Temp.next();
    }

    // /////////////////////////////////////////////////////
    // expressions
    @Override
    public void visit(Add e) {
        binaryConstFoldOpt(e);
    }

    @Override
    public void visit(And e) {
        if (e.right instanceof False || e.left instanceof False) {
            curChangeExp = new False();
            expAns = -1;
            globalOpt = true;
        } else if (e.right instanceof True && e.left instanceof True) {
            curChangeExp = new True();
            expAns = 1;
            globalOpt = true;
        } else if (e.right instanceof True) {
            curChangeExp = e.left;
            globalOpt = true;
        } else if (e.left instanceof True) {
            globalOpt = true;
            curChangeExp = e.right;
        } else {
            boolean expModifyBk = expModify;
            expModify = false;
            e.left.accept(this);
            Ast.Exp.T left = curChangeExp;
            e.right.accept(this);
            Ast.Exp.T right = curChangeExp;
            if (expModify)
                curChangeExp = new And(left, right);
            else {
                expModify = expModifyBk;
                curChangeExp = e;
            }
        }
    }

    @Override
    public void visit(ArraySelect e) {
        boolean expModifyBk = expModify;
        e.index.accept(this);
        if (expModify) {
            curChangeExp = new ArraySelect(curChangeExp, e.array);
        } else {
            curChangeExp = e;
            expModify = expModifyBk;
        }
    }

    @Override
    public void visit(Call e) {
        boolean expModifyBk = expModify;
        for (Ast.Exp.T arg : e.args
        ) {
            if (arg instanceof Id)
                checkDec(((Id) arg).id);
        }
        e.exp.accept(this);
        if (expModify) {
            curChangeExp = new Call(curChangeExp, e.id, e.args);
        } else {
            curChangeExp = e;
            expModify = expModifyBk;
        }
        return;
    }

    @Override
    public void visit(False e) {
        expAns = -1;
        curChangeExp = e;
    }

    @Override
    public void visit(Id e) {
        curChangeExp = e;
        checkDec(e.id);
        return;
    }

    @Override
    public void visit(Length e) {
        curChangeExp = e;
    }

    @Override
    public void visit(Lt e) {
        boolean expModifyBk = expModify;
        e.left.accept(this);
        Ast.Exp.T left = curChangeExp;
        e.right.accept(this);
        Ast.Exp.T right = curChangeExp;

        if (left instanceof Num && right instanceof Num) {
            if (((Num) left).num < ((Num) right).num) {
                curChangeExp = new True();
                expAns = 1;
                globalOpt = true;
            } else {
                curChangeExp = new False();
                expAns = -1;
                globalOpt = true;
            }
        } else {
            if (expModify)
                curChangeExp = new Lt(left, right);
            else {
                curChangeExp = e;
                expModify = expModifyBk;
            }
        }
        return;
    }

    @Override
    public void visit(NewIntArray e) {
        boolean expModifyBk = expModify;
        expModify = false;
        e.exp.accept(this);
        if (expModify)
            curChangeExp = new NewIntArray(curChangeExp);
        else {
            curChangeExp = e;
            expModify = expModifyBk;
        }
    }

    @Override
    public void visit(NewObject e) {
        curChangeExp = e;
        return;
    }

    @Override
    public void visit(Not e) {
        if (e.exp instanceof True) {
            curChangeExp = new False();
            expAns = 0;
            expModify = true;
            globalOpt = true;
        } else if (e.exp instanceof False) {
            expAns = 1;
            curChangeExp = new True();
            expModify = true;
            globalOpt = true;
        } else {
            e.exp.accept(this);
        }
    }

    @Override
    public void visit(Num e) {
        curChangeExp = e;
        return;
    }

    @Override
    public void visit(Sub e) {
        binaryConstFoldOpt(e);
    }

    @Override
    public void visit(This e) {
        curChangeExp = e;
        return;
    }

    @Override
    public void visit(Times e) {
        binaryConstFoldOpt(e);
    }

    @Override
    public void visit(True e) {
        expAns = 1;
        curChangeExp = e;
    }

    // statements
    @Override
    public void visit(Assign s) {
        checkDec(s.id);
        expModify = false;
        s.exp.accept(this);
        if (expModify) {
            curChangeStm = new Assign(s.id, curChangeExp);
            Assign tmp = (Assign) curChangeStm;
            tmp.type = s.type;
            stmModify = true;
        } else
            curChangeStm = s;
        return;
    }

    @Override
    public void visit(AssignArray s) {
        expModify = false;
        checkDec(s.id);
        s.index.accept(this);
        Ast.Exp.T index = curChangeExp;
        s.exp.accept(this);
        Ast.Exp.T exp = curChangeExp;
        if (expModify) {
            curChangeStm = new AssignArray(s.id, index, exp);
            stmModify = true;
        } else
            curChangeStm = s;
    }

    @Override
    public void visit(Block s) {
        //这里有bug，如果一个block里有多个block，
        // 最后一个block不存在优化后的stm，
        // 这样会掩盖前面优化对stmmodify的修改。
        LinkedList<Ast.Stm.T> stms = new LinkedList<>();
        boolean blockOpt = false;
        stmModify = false;
        for (Ast.Stm.T stm : s.stms
        ) {
            stm.accept(this);
            stms.add(curChangeStm);
            if (stmModify)
                blockOpt = true;
        }
        if (blockOpt) {
            curChangeStm = new Block(stms);
            stmModify = true;
        } else
            curChangeStm = s;
    }

    @Override
    //这里没做常量折叠和代数化简等优化，原因是代码写起来会很麻烦
    //但是在一遍优化后，if语句可能会直接被化简为其他类型的stm，
    //这个时候可以再做上述的优化。
    public void visit(If s) {
        expAns = 0;
        expModify = false;
        s.condition.accept(this);
        Ast.Exp.T condition = curChangeExp;
        boolean condModify = expModify;
        if (expAns == 1) {
            curChangeStm = s.thenn;
            globalOpt = true;
            stmModify = true;
        } else if (expAns == -1) {
            curChangeStm = s.elsee;
            globalOpt = true;
            stmModify = true;
        } else {
            //这里有些危险,对于多条stm，如果这个if没做优化
            // 同样会掩盖之前做的优化，将stmmodify覆盖。
            boolean stmModifyBk = stmModify;
            stmModify = false;
            s.thenn.accept(this);
            Ast.Stm.T thenn = curChangeStm;
            s.elsee.accept(this);
            Ast.Stm.T elsee = curChangeStm;
            if (stmModify || condModify) {
                curChangeStm = new If(condition, thenn, elsee);
            } else {
                curChangeStm = s;
                stmModify = stmModifyBk;
            }
        }
        return;
    }

    @Override
    public void visit(Print s) {
        expModify = false;
        s.exp.accept(this);
        if (expModify) {
            curChangeStm = new Print(curChangeExp);
            stmModify = true;
        } else
            curChangeStm = s;
        return;
    }

    @Override
    public void visit(While s) {
        expAns = 0;
        s.condition.accept(this);
        if (expAns != -1) {
            boolean stmModifyBk = stmModify;
            stmModify = false;
            s.stm.accept(this);
            if (stmModify) {
                curChangeStm = new While(curChangeExp, curChangeStm);
                stmModify = true;
            } else {
                curChangeStm = s;
                stmModify = stmModifyBk;
            }
        } else {
            globalOpt = true;
            stmModify = true;
            curChangeStm = null;
        }
    }

    // type
    @Override
    public void visit(Boolean t) {
    }

    @Override
    public void visit(ClassType t) {
    }

    @Override
    public void visit(Int t) {
    }

    @Override
    public void visit(IntArray t) {
    }

    // dec
    @Override
    public void visit(DecSingle d) {
        return;
    }

    // method
    @Override
    public void visit(MethodSingle m) {
        curUsedMethodDecs = new HashMap<>();
        curDecMethodDecs = new HashMap<>();
        curStms = new LinkedList<>();

        putDecsInLinkedListToMap(m.locals, curDecMethodDecs);
        for (Ast.Stm.T stm : m.stms
        ) {
            stm.accept(this);
            if (curChangeStm != null)
                curStms.add(curChangeStm);
        }

        LinkedList<Ast.Dec.T> usedLocals = new LinkedList<>();
        putDecsInMapToLinkedList(usedLocals, curUsedMethodDecs);
        if (usedLocals.size() == m.locals.size())
            curMethods.add(new MethodSingle(m.retType, m.id, m.formals, m.locals, curStms, m.retExp));
        else
            curMethods.add(new MethodSingle(m.retType, m.id, m.formals, usedLocals, curStms, m.retExp));
        return;
    }

    // class
    @Override
    public void visit(ClassSingle c) {
        curUsedClassDecs = new HashMap<>();
        curDecClassDecs = new HashMap<>();
        curMethods = new LinkedList<>();

        putDecsInLinkedListToMap(c.decs, curDecClassDecs);

        for (Ast.Method.T method : c.methods
        ) {
            method.accept(this);
        }

        LinkedList<Ast.Dec.T> decs = new LinkedList<>();
        putDecsInMapToLinkedList(decs, curUsedClassDecs);
        newClasss.add(new ClassSingle(c.id, c.extendss, decs, curMethods));
        return;
    }

    // main class
    @Override
    public void visit(MainClassSingle c) {
        stmModify = false;
        c.stm.accept(this);
        if (stmModify)
            mainClass = new MainClassSingle(c.id, c.arg, curChangeStm);
        else
            mainClass = c;
        return;
    }

    // program
    @Override
    public void visit(ProgramSingle p) {
        // You should comment out this line of code:
        this.program = p;
        while (globalOpt) {
            newClasss = new LinkedList<>();
            globalOpt = false;
            ProgramSingle prog = (ProgramSingle) this.program;
            prog.mainClass.accept(this);
            for (Ast.Class.T classs : prog.classes
            ) {
                classs.accept(this);
            }
            this.program = new ProgramSingle(mainClass, newClasss);
        }


        if (control.Control.trace.equals("ast.DeadCode")) {
            System.out.println("before optimization:");
            ast.PrettyPrintVisitor pp = new ast.PrettyPrintVisitor();
            p.accept(pp);
            System.out.println("after optimization:");
            this.program.accept(pp);
        }
    }
}
