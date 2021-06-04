package codegen.C;

import codegen.C.Ast.Class.ClassSingle;
import codegen.C.Ast.Dec;
import codegen.C.Ast.Dec.DecSingle;
import codegen.C.Ast.Exp;
import codegen.C.Ast.Exp.Add;
import codegen.C.Ast.Exp.And;
import codegen.C.Ast.Exp.ArraySelect;
import codegen.C.Ast.Exp.Call;
import codegen.C.Ast.Exp.Id;
import codegen.C.Ast.Exp.Length;
import codegen.C.Ast.Exp.Lt;
import codegen.C.Ast.Exp.NewIntArray;
import codegen.C.Ast.Exp.NewObject;
import codegen.C.Ast.Exp.Not;
import codegen.C.Ast.Exp.Num;
import codegen.C.Ast.Exp.Sub;
import codegen.C.Ast.Exp.This;
import codegen.C.Ast.Exp.Times;
import codegen.C.Ast.MainMethod.MainMethodSingle;
import codegen.C.Ast.Method;
import codegen.C.Ast.Method.MethodSingle;
import codegen.C.Ast.Program.ProgramSingle;
import codegen.C.Ast.Stm;
import codegen.C.Ast.Stm.Assign;
import codegen.C.Ast.Stm.AssignArray;
import codegen.C.Ast.Stm.Block;
import codegen.C.Ast.Stm.If;
import codegen.C.Ast.Stm.Print;
import codegen.C.Ast.Stm.While;
import codegen.C.Ast.Type.ClassType;
import codegen.C.Ast.Type.Int;
import codegen.C.Ast.Type.IntArray;
import codegen.C.Ast.Vtable;
import codegen.C.Ast.Vtable.VtableSingle;
import control.Control;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class PrettyPrintVisitor implements Visitor {
    private int indentLevel;
    private java.io.BufferedWriter writer;
    private static HashMap<String, Boolean> localVar = new HashMap<>();

    public PrettyPrintVisitor() {
        this.indentLevel = 2;
    }

    private void indent() {
        this.indentLevel += 2;
    }

    private void unIndent() {
        this.indentLevel -= 2;
    }

    private void printSpaces() {
        int i = this.indentLevel;
        while (i-- != 0)
            this.say(" ");
    }

    private void sayln(String s) {
        say(s);
        try {
            this.writer.write("\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void saylnWithSpace(String s) {
        printSpaces();
        sayln(s);
    }

    private HashSet<String> searchMethodRefVar(LinkedList<Dec.T> vars) {
        HashSet<String> refVarName = new HashSet<>();
        for (int i = 0; i < vars.size(); i++) {
            Dec.T d = vars.get(i);
            if (d instanceof DecSingle) {
                if (((DecSingle) d).type instanceof IntArray) {
                    saylnWithSpace("int [] " + ((DecSingle) d).id + ";");
                    refVarName.add(((DecSingle) d).id);
                } else if (((DecSingle) d).type instanceof ClassType) {
                    saylnWithSpace("struct " + ((ClassType) ((DecSingle) d).type).id + "* " + ((DecSingle) d).id + ";");
                    refVarName.add(((DecSingle) d).id);
                }
            }
        }
        return refVarName;
    }

    private void say(String s) {
        try {
            this.writer.write(s);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // /////////////////////////////////////////////////////
    // expressions
    @Override
    public void visit(Add e) {
        e.left.accept(this);
        this.say("+");
        e.right.accept(this);
    }

    @Override
    public void visit(And e) {
        e.left.accept(this);
        this.say("&&");
        e.right.accept(this);
    }

    @Override
    public void visit(ArraySelect e) {
        e.array.accept(this);
        this.say("[");
        e.index.accept(this);
        this.say("]");
    }

    @Override
    public void visit(Call e) {
        this.say("(" + e.assign + "=");
        e.exp.accept(this);
        this.say(",");
        this.say("frame." + e.assign + "=" + e.assign);
        this.say(", ");
        this.say(e.assign + "->vptr->" + e.id + "(" + e.assign);
        int size = e.args.size();
        if (size == 0) {
            this.say("))");
            return;
        }
        for (Exp.T x : e.args) {
            this.say(", ");
            x.accept(this);
        }
        this.say("))");
        return;
    }

    @Override
    public void visit(Id e) {
        if (localVar.containsKey(e.id))
            this.say(e.id);
        else
            this.say("this->" + e.id);
    }

    @Override
    public void visit(Length e) {

        this.say("(" + e.assign + "=");
        e.array.accept(this);
        this.say(", ");
        say("*((int*)(" + e.assign);
        say(")-1)");
        this.say(")");

    }

    @Override
    public void visit(Lt e) {
        e.left.accept(this);
        this.say(" < ");
        e.right.accept(this);
        return;
    }

    @Override
    public void visit(NewIntArray e) {
        say("Tiger_new_array(");
        e.exp.accept(this);
        say(")");
    }

    @Override
    public void visit(NewObject e) {
        this.say("((struct " + e.id + "*)(Tiger_new (&" + e.id
                + "_vtable_, sizeof(struct " + e.id + "))))");
        return;
    }

    @Override
    public void visit(Not e) {
        say("!(");
        e.exp.accept(this);
        say(")");
    }

    @Override
    public void visit(Num e) {
        this.say(Integer.toString(e.num));
        return;
    }

    @Override
    public void visit(Sub e) {
        e.left.accept(this);
        this.say(" - ");
        e.right.accept(this);
        return;
    }

    @Override
    public void visit(This e) {
        this.say("this");
    }

    @Override
    public void visit(Times e) {
        e.left.accept(this);
        this.say(" * ");
        e.right.accept(this);
        return;
    }

    // statements
    @Override
    public void visit(Assign s) {
        this.printSpaces();
        if (localVar.containsKey(s.id))
            this.say(s.id + " = ");
        else
            this.say("this->" + s.id + "=");
        s.exp.accept(this);
        this.sayln(";");
        if (s.type instanceof ClassType || s.type instanceof IntArray) {
            if (localVar.containsKey(s.id))
                saylnWithSpace("frame." + s.id + "=" + s.id + ";");
            else
                saylnWithSpace("frame.this->" + s.id + "=this->" + s.id + ";");
        }
        return;
    }

    @Override
    public void visit(AssignArray s) {
        this.printSpaces();
        if (localVar.containsKey(s.id))
            say(s.id + "[");
        else
            say("this->" + s.id + "[");
        s.index.accept(this);
        say("]=");
        s.exp.accept(this);
        sayln(";");
    }

    @Override
    public void visit(Block s) {
        this.printSpaces();
        sayln("{");
        this.indent();
        for (Stm.T stm :
                s.stms) {
            stm.accept(this);
        }
        this.unIndent();
        this.printSpaces();
        sayln("}");
    }

    @Override
    public void visit(If s) {
        this.printSpaces();
        this.say("if (");
        s.condition.accept(this);
        this.sayln(")");
        this.indent();
        s.thenn.accept(this);
        this.unIndent();
        this.sayln("");
        this.printSpaces();
        this.sayln("else");
        this.indent();
        s.elsee.accept(this);
        this.sayln("");
        this.unIndent();
        return;
    }

    @Override
    public void visit(Print s) {
        this.printSpaces();
        this.say("System_out_println (");
        s.exp.accept(this);
        this.sayln(");");
        return;
    }

    @Override
    public void visit(While s) {
        this.printSpaces();
        this.say("while(");
        s.condition.accept(this);
        this.sayln(")");
        this.printSpaces();
        this.sayln("{");
        this.indent();
        s.body.accept(this);
        this.unIndent();
        printSpaces();
        sayln("}");
    }

    // type
    @Override
    public void visit(ClassType t) {
        this.say("struct " + t.id + " *");
    }

    @Override
    public void visit(Int t) {
        this.say("int");
    }

    @Override
    public void visit(IntArray t) {
        this.say("int*");
    }

    // dec
    @Override
    public void visit(DecSingle d) {
        d.type.accept(this);
        say(" " + d.id);
        sayln(";");
    }

    // method
    @Override
    public void visit(MethodSingle m) {
        sayln("struct " + m.classId + "_" + m.id + "_gc_frame{");
        indent();
        saylnWithSpace("double length;");
        saylnWithSpace("void *gc_frame_prev;");
        HashSet<String> argsRefVar = searchMethodRefVar(m.formals);
        searchMethodRefVar(m.locals);
        unIndent();
        sayln("};");


        m.retType.accept(this);
        this.say(" " + m.classId + "_" + m.id + "(");
        int size = m.formals.size();
        localVar.clear();
        for (Dec.T d : m.formals) {
            DecSingle dec = (DecSingle) d;
            localVar.put(dec.id, true);
            size--;
            dec.type.accept(this);
            this.say(" " + dec.id);
            if (size > 0)
                this.say(", ");
        }
        this.sayln(")");
        this.sayln("{");

        saylnWithSpace("struct " + m.classId + "_" + m.id + "_gc_frame frame;");
        saylnWithSpace("frame.length=" + (m.formals.size() + m.locals.size()) + ";");
        saylnWithSpace("frame.gc_frame_prev=gc_frame_prev;");
        saylnWithSpace("gc_frame_prev=&frame;");

        for (Dec.T d : m.locals) {
            DecSingle dec = (DecSingle) d;
            localVar.put(dec.id, true);
            this.say("  ");
            dec.type.accept(this);
            this.say(" " + dec.id + ";\n");
        }

        for (String argsName :
                argsRefVar)
            saylnWithSpace("frame." + argsName + "=" + argsName + ";");

        this.sayln("");
        for (Stm.T s : m.stms)
            s.accept(this);
        saylnWithSpace("gc_frame_prev=frame.gc_frame_prev;");
        this.say("  return ");
        m.retExp.accept(this);
        this.sayln(";");
        this.sayln("}");
        return;
    }

    @Override
    public void visit(MainMethodSingle m) {
        sayln("struct main" + "_gc_frame{");
        indent();
        saylnWithSpace("double length;");
        saylnWithSpace("void *gc_frame_prev;");
        searchMethodRefVar(m.locals);

        unIndent();
        sayln("};");

        localVar.clear();
        this.sayln("int main ()");
        this.sayln("{");

        saylnWithSpace("struct main_gc_frame frame;");
        saylnWithSpace("frame.gc_frame_prev=gc_frame_prev;");
        saylnWithSpace("gc_frame_prev=&frame;");
        saylnWithSpace("frame.length=" + m.locals.size() + ";");
        printSpaces();
        sayln("vtableInit();");
        for (Dec.T dec : m.locals) {
            this.say("  ");
            DecSingle d = (DecSingle) dec;
            localVar.put(d.id, true);
            d.type.accept(this);
            this.say(" ");
            this.sayln(d.id + ";");
        }
        m.stm.accept(this);
        saylnWithSpace("gc_frame_prev=frame.gc_frame_prev;");
        this.sayln("}\n");
        return;
    }

    // vtables
    @Override
    public void visit(VtableSingle v) {
        this.sayln("struct " + v.id + "_vtable");
        this.sayln("{");
        for (codegen.C.Ftuple t : v.ms) {
            this.say("  ");
            t.ret.accept(this);
            this.sayln(" (*" + t.id + ")();");
        }
        this.sayln("};\n");
        return;
    }

    private void decVtable(VtableSingle v) {
        this.sayln("struct " + v.id + "_vtable " + v.id + "_vtable_;");
    }

    private void outputVtable(VtableSingle v) {
        for (codegen.C.Ftuple t : v.ms) {
            printSpaces();
            this.sayln(v.id + "_vtable_" + "." + t.id + "=" + t.classs + "_" + t.id + ";");
        }
        return;
    }

    // class
    @Override
    public void visit(ClassSingle c) {
        this.sayln("struct " + c.id);
        this.sayln("{");
        this.sayln("  struct " + c.id + "_vtable *vptr;");
        for (codegen.C.Tuple t : c.decs) {
            this.say("  ");
            t.type.accept(this);
            this.say(" ");
            this.sayln(t.id + ";");
        }
        this.sayln("};");
        return;
    }

    // program
    @Override
    public void visit(ProgramSingle p) {
        // we'd like to output to a file, rather than the "stdout".
        try {
            String outputName = "mini_java_main.c";
//            if (Control.ConCodeGen.outputName != null)
//                outputName = Control.ConCodeGen.outputName;
//            else if (Control.ConCodeGen.fileName != null)
//                outputName = Control.ConCodeGen.fileName + ".c";
//            else
//                outputName = "a.c";

            this.writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(outputName)));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        this.sayln("// This is automatically generated by the Tiger compiler.");
        this.sayln("// Do NOT modify!\n");

        this.sayln("void* gc_frame_prev;");

        this.sayln("// structures");
        for (codegen.C.Ast.Class.T c : p.classes) {
            c.accept(this);
        }

        this.sayln("// vtables structures");
        for (Vtable.T v : p.vtables) {
            v.accept(this);
        }
        this.sayln("");

        for (Vtable.T v : p.vtables) {
            decVtable((VtableSingle) v);
        }
        this.sayln("");

        this.sayln("// methods");
        for (Method.T m : p.methods) {
            m.accept(this);
        }
        this.sayln("");

        this.sayln("// vtables");
        this.sayln("void vtableInit()");
        this.sayln("{");
        indent();
        for (Vtable.T v : p.vtables) {
            outputVtable((VtableSingle) v);
        }
        unIndent();
        sayln("}");
        this.sayln("");

        this.sayln("// main method");
        p.mainMethod.accept(this);
        this.sayln("");

        this.say("\n\n");

        try {
            this.writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

}