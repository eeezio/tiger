package slp;

import slp.Slp.Exp;
import slp.Slp.Exp.Eseq;
import slp.Slp.Exp.Id;
import slp.Slp.Exp.Num;
import slp.Slp.Exp.Op;
import slp.Slp.ExpList;
import slp.Slp.Stm;
import util.Bug;

import java.io.FileWriter;
import java.util.HashSet;


class Table {
  String id;
  int value;
  Table tail;

  Table(String i, int v, Table t) {
    id = i;
    value = v;
    tail = t;
  }

  int lookup(String key) throws Exception {
    Table tmp = this;
    int ans = 0;
    while (tmp != null) {
      if (tmp.id.equals(key)) {
        ans = tmp.value;
        break;
      }
      tmp = tmp.tail;
    }
    if (tmp == null) {
      throw new Exception("查找不到当前符号！");
    }
    return ans;
  }
}

class IntAndTable {
  int i;
  Table t;

  IntAndTable(int ii, Table tt) {
    i = ii;
    t = tt;
  }
}


public class Main {
  // ///////////////////////////////////////////
  // maximum number of args

  private int maxArgsExp(Exp.T exp) {
//        new Todo();
    if (exp instanceof Num) {
      return 1;
    } else if (exp instanceof Id) {
      return 1;
    } else if (exp instanceof Op) {
      int n1 = maxArgsExp(((Op) exp).left);
      int n2 = maxArgsExp(((Op) exp).right);
      return n1 >= n2 ? n1 : n2;
    } else {
      Eseq tmp = (Eseq) exp;
      int n1 = maxArgsStm(tmp.stm);
      int n2 = maxArgsExp(tmp.exp);
      return n1 >= n2 ? n1 : n2;
    }
//        return -1;
  }

  private int maxArgsStm(Stm.T stm) {
    if (stm instanceof Stm.Compound) {
      Stm.Compound s = (Stm.Compound) stm;
      int n1 = maxArgsStm(s.s1);
      int n2 = maxArgsStm(s.s2);

      return n1 >= n2 ? n1 : n2;
    } else if (stm instanceof Stm.Assign) {
//      new Todo();
      Stm.Assign s = (Stm.Assign) stm;
      int n = maxArgsExp(s.exp);
      return 0 >= n ? 0 : n;
    } else if (stm instanceof Stm.Print) {
//            new Todo();
      Stm.Print s = (Stm.Print) stm;
      int n0 = 0;//用于记录如果参数内包含print语句的话，有几个参数
      int n1 = 1;//用于记录该条print语句有几个参数
      int n2 = 0;
      if (s.explist instanceof ExpList.Pair) {
        ExpList.Pair pair = (ExpList.Pair) s.explist;
        Exp.T exp = pair.exp;
        n0 = maxArgsExp(exp);
        while (pair.list instanceof ExpList.Pair) {
          //tmp 为一个完整的pairexplist，包含左右两部。
          ExpList.Pair tmp = (ExpList.Pair) pair.list;
          Exp.T exp2 = tmp.exp;
          n2 = maxArgsExp(exp2);
          n1 += 1;
          n0 = n0 >= n2 ? n0 : n2;
          pair = tmp;
        }
        //跳出循环后的pair已经变成了lastExpList
        n1 += 1;
        n2 = maxArgsExp(pair.exp);
        n0 = n0 >= n2 ? n0 : n2;
        return n0 >= n1 ? n0 : n1;
      }

      return -1;
    } else
      new Bug();
    return 0;
  }

  // ////////////////////////////////////////
  // interpreter


  private IntAndTable interpExp(Exp.T exp, Table t) throws Exception {
//        new Todo();
    if (exp instanceof Id) {
      int target = t.lookup(((Id) exp).id);
      return new IntAndTable(target, t);
    } else if (exp instanceof Num) {
      return new IntAndTable(((Num) exp).num, t);
    } else if (exp instanceof Op) {
      IntAndTable intAndTable1 = interpExp(((Op) exp).left, t);
      IntAndTable intAndTable2 = interpExp(((Op) exp).right, t);
      if (((Op) exp).op == Exp.OP_T.ADD) {
        return new IntAndTable(intAndTable1.i + intAndTable2.i, t);
      } else if (((Op) exp).op == Exp.OP_T.SUB) {
        return new IntAndTable(intAndTable1.i - intAndTable2.i, t);
      } else if (((Op) exp).op == Exp.OP_T.DIVIDE) {
        return new IntAndTable(intAndTable1.i / intAndTable2.i, t);
      } else {
        return new IntAndTable(intAndTable1.i * intAndTable2.i, t);
      }
    } else {
      Table table = interpStm(((Eseq) exp).stm, t);
      return interpExp(((Eseq) exp).exp,table);
    }
  }

  private Table interpStm(Stm.T prog, Table table) throws Exception {
    if (prog instanceof Stm.Compound) {
//            new Todo();
      Table table1 = interpStm(((Stm.Compound) prog).s1, table);
      Table table2 = interpStm(((Stm.Compound) prog).s2, table1);
      return table2;
    } else if (prog instanceof Stm.Assign) {
//            new Todo();
      IntAndTable intAndTable = interpExp(((Stm.Assign) prog).exp, table);
      Table newTable = new Table(((Stm.Assign) prog).id, intAndTable.i, intAndTable.t);
      return newTable;
    } else if (prog instanceof Stm.Print) {
//            new Todo();
      ExpList.T list = ((Stm.Print) prog).explist;
      Table table1 = table;
      while (list instanceof ExpList.Pair) {
        Exp.T exp = ((ExpList.Pair) list).exp;
        IntAndTable intAndTable = interpExp(exp, table1);
        System.out.printf(String.valueOf(intAndTable.i) + " ");
        table1 = intAndTable.t;
        list = ((ExpList.Pair) list).list;
      }
      Exp.T exp = ((ExpList.Last) list).exp;
      IntAndTable intAndTable = interpExp(exp, table1);
      System.out.println(intAndTable.i);
      return intAndTable.t;
    } else
      new Bug();
    return null;
  }

  // ////////////////////////////////////////
  // compile
  HashSet<String> ids;
  StringBuffer buf;

  private void emit(String s) {
    buf.append(s);
  }

  private void compileExp(Exp.T exp) {
    if (exp instanceof Id) {
      Exp.Id e = (Exp.Id) exp;
      String id = e.id;

      emit("\tmovl\t" + id + ", %eax\n");
    } else if (exp instanceof Num) {
      Exp.Num e = (Exp.Num) exp;
      int num = e.num;

      emit("\tmovl\t$" + num + ", %eax\n");
    } else if (exp instanceof Op) {
      Exp.Op e = (Exp.Op) exp;
      Exp.T left = e.left;
      Exp.T right = e.right;
      Exp.OP_T op = e.op;

      switch (op) {
        case ADD:
          compileExp(left);
          emit("\tpushl\t%eax\n");
          compileExp(right);
          emit("\tpopl\t%edx\n");
          emit("\taddl\t%edx, %eax\n");
          break;
        case SUB:
          compileExp(left);
          emit("\tpushl\t%eax\n");
          compileExp(right);
          emit("\tpopl\t%edx\n");
          emit("\tsubl\t%eax, %edx\n");
          emit("\tmovl\t%edx, %eax\n");
          break;
        case TIMES:
          compileExp(left);
          emit("\tpushl\t%eax\n");
          compileExp(right);
          emit("\tpopl\t%edx\n");
          emit("\timul\t%edx\n");
          break;
        case DIVIDE:
          compileExp(left);
          emit("\tpushl\t%eax\n");
          compileExp(right);
          emit("\tpopl\t%edx\n");
          emit("\tmovl\t%eax, %ecx\n");
          emit("\tmovl\t%edx, %eax\n");
          emit("\tcltd\n");
          emit("\tdiv\t%ecx\n");
          break;
        default:
          new Bug();
      }
    } else if (exp instanceof Eseq) {
      Eseq e = (Eseq) exp;
      Stm.T stm = e.stm;
      Exp.T ee = e.exp;

      compileStm(stm);
      compileExp(ee);
    } else
      new Bug();
  }

  private void compileExpList(ExpList.T explist) {
    if (explist instanceof ExpList.Pair) {
      ExpList.Pair pair = (ExpList.Pair) explist;
      Exp.T exp = pair.exp;
      ExpList.T list = pair.list;

      compileExp(exp);
      emit("\tpushl\t%eax\n");
      emit("\tpushl\t$slp_format\n");
      emit("\tcall\tprintf\n");
      emit("\taddl\t$4, %esp\n");
      compileExpList(list);
    } else if (explist instanceof ExpList.Last) {
      ExpList.Last last = (ExpList.Last) explist;
      Exp.T exp = last.exp;

      compileExp(exp);
      emit("\tpushl\t%eax\n");
      emit("\tpushl\t$slp_format\n");
      emit("\tcall\tprintf\n");
      emit("\taddl\t$4, %esp\n");
    } else
      new Bug();
  }

  private void compileStm(Stm.T prog) {
    if (prog instanceof Stm.Compound) {
      Stm.Compound s = (Stm.Compound) prog;
      Stm.T s1 = s.s1;
      Stm.T s2 = s.s2;

      compileStm(s1);
      compileStm(s2);
    } else if (prog instanceof Stm.Assign) {
      Stm.Assign s = (Stm.Assign) prog;
      String id = s.id;
      Exp.T exp = s.exp;

      ids.add(id);
      compileExp(exp);
      emit("\tmovl\t%eax, " + id + "\n");
    } else if (prog instanceof Stm.Print) {
      Stm.Print s = (Stm.Print) prog;
      ExpList.T explist = s.explist;

      compileExpList(explist);
      emit("\tpushl\t$newline\n");
      emit("\tcall\tprintf\n");
      emit("\taddl\t$4, %esp\n");
    } else
      new Bug();
  }

  // ////////////////////////////////////////
  public void doit(Stm.T prog) throws Exception {
    // return the maximum number of arguments
    if (Control.ConSlp.action == Control.ConSlp.T.ARGS) {
      int numArgs = maxArgsStm(prog);
      System.out.println(numArgs);
    }

    // interpret a given program
    if (Control.ConSlp.action == Control.ConSlp.T.INTERP) {
      interpStm(prog, null);
    }

    // compile a given SLP program to x86
    if (Control.ConSlp.action == Control.ConSlp.T.COMPILE) {
      ids = new HashSet<String>();
      buf = new StringBuffer();

      compileStm(prog);
      try {
        // FileOutputStream out = new FileOutputStream();
        FileWriter writer = new FileWriter("slp_gen.s");
        writer
                .write("// Automatically generated by the Tiger compiler, do NOT edit.\n\n");
        writer.write("\t.data\n");
        writer.write("slp_format:\n");
        writer.write("\t.string \"%d \"\n");
        writer.write("newline:\n");
        writer.write("\t.string \"\\n\"\n");
        for (String s : this.ids) {
          writer.write(s + ":\n");
          writer.write("\t.int 0\n");
        }
        writer.write("\n\n\t.text\n");
        writer.write("\t.globl main\n");
        writer.write("main:\n");
        writer.write("\tpushl\t%ebp\n");
        writer.write("\tmovl\t%esp, %ebp\n");
        writer.write(buf.toString());
        writer.write("\tleave\n\tret\n\n");
        writer.close();
        Process child = Runtime.getRuntime().exec("gcc slp_gen.s");
        child.waitFor();
        if (!Control.ConSlp.keepasm)
          Runtime.getRuntime().exec("rm -rf slp_gen.s");
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(0);
      }
      // System.out.println(buf.toString());
    }
  }
}
