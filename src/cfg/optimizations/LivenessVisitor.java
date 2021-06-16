package cfg.optimizations;

import cfg.Cfg.*;
import cfg.Cfg.Block.BlockSingle;
import cfg.Cfg.Class.ClassSingle;
import cfg.Cfg.Dec.DecSingle;
import cfg.Cfg.MainMethod.MainMethodSingle;
import cfg.Cfg.Method.MethodSingle;
import cfg.Cfg.Operand.Int;
import cfg.Cfg.Operand.Var;
import cfg.Cfg.Program.ProgramSingle;
import cfg.Cfg.Stm.*;
import cfg.Cfg.Transfer.Goto;
import cfg.Cfg.Transfer.If;
import cfg.Cfg.Transfer.Return;
import cfg.Cfg.Type.ClassType;
import cfg.Cfg.Type.IntArrayType;
import cfg.Cfg.Type.IntType;
import cfg.Cfg.Vtable.VtableSingle;

import java.util.*;

public class LivenessVisitor implements cfg.Visitor {
    // gen, kill for one statement
    private HashSet<String> oneStmGen;
    private HashSet<String> oneStmKill;

    // gen, kill for one transfer
    private HashSet<String> oneTransferGen;
    private HashSet<String> oneTransferKill;

    // gen, kill for statements
    private HashMap<Stm.T, HashSet<String>> stmGen;
    private HashMap<Stm.T, HashSet<String>> stmKill;

    // gen, kill for transfers
    private HashMap<Transfer.T, HashSet<String>> transferGen;
    private HashMap<Transfer.T, HashSet<String>> transferKill;

    // gen, kill for blocks
    private HashMap<Block.T, HashSet<String>> blockGen;
    private HashMap<Block.T, HashSet<String>> blockKill;

    // liveIn, liveOut for blocks
    private HashMap<Block.T, HashSet<String>> blockLiveIn;
    private HashMap<Block.T, HashSet<String>> blockLiveOut;

    // liveIn, liveOut for statements
    public HashMap<Stm.T, HashSet<String>> stmLiveIn;
    public HashMap<Stm.T, HashSet<String>> stmLiveOut;

    // liveIn, liveOut for transfer
    public HashMap<Transfer.T, HashSet<String>> transferLiveIn;
    public java.util.HashMap<Transfer.T, java.util.HashSet<String>> transferLiveOut;

    // As you will walk the tree for many times, so
    // it will be useful to recored which is which

    enum Liveness_Kind_t {
        None, StmGenKill, StmKill, StmGen, TransferGen, TransferKill, BlockGenKill, BlockInOut, StmInOut,
    }

    private Liveness_Kind_t kind = Liveness_Kind_t.None;

    public LivenessVisitor() {
        this.oneStmGen = new HashSet<>();
        this.oneStmKill = new java.util.HashSet<>();

        this.oneTransferGen = new java.util.HashSet<>();
        this.oneTransferKill = new java.util.HashSet<>();

        this.stmGen = new java.util.HashMap<>();
        this.stmKill = new java.util.HashMap<>();

        this.transferGen = new java.util.HashMap<>();
        this.transferKill = new java.util.HashMap<>();

        this.blockGen = new java.util.HashMap<>();
        this.blockKill = new java.util.HashMap<>();

        this.blockLiveIn = new java.util.HashMap<>();
        this.blockLiveOut = new java.util.HashMap<>();

        this.stmLiveIn = new java.util.HashMap<>();
        this.stmLiveOut = new java.util.HashMap<>();

        this.transferLiveIn = new java.util.HashMap<>();
        this.transferLiveOut = new java.util.HashMap<>();

        this.kind = Liveness_Kind_t.None;
    }

    // /////////////////////////////////////////////////////
    // utilities

    private java.util.HashSet<String> getOneStmGenAndClear() {
        java.util.HashSet<String> temp = this.oneStmGen;
        this.oneStmGen = new java.util.HashSet<>();
        return temp;
    }

    private java.util.HashSet<String> getOneStmKillAndClear() {
        java.util.HashSet<String> temp = this.oneStmKill;
        this.oneStmKill = new java.util.HashSet<>();
        return temp;
    }

    private java.util.HashSet<String> getOneTransferGenAndClear() {
        java.util.HashSet<String> temp = this.oneTransferGen;
        this.oneTransferGen = new java.util.HashSet<>();
        return temp;
    }

    private java.util.HashSet<String> getOneTransferKillAndClear() {
        java.util.HashSet<String> temp = this.oneTransferKill;
        this.oneTransferKill = new java.util.HashSet<>();
        return temp;
    }


    private boolean checkFixPoint(LinkedHashMap<Block.T, Integer> fixPointFlag, StringBuffer bitVector) {
        int iter = 0;
        for (Integer i : fixPointFlag.values()
        ) {
            if (i.intValue() != Integer.valueOf(bitVector.charAt(iter)))
                return true;
            iter += 1;
        }
        return false;
    }

    private void initFixPointFlagAndBitVector(LinkedHashMap<Block.T, Integer> fixPointFlag, StringBuffer bitVector) {
        int iter = 0;
        for (Integer i : fixPointFlag.values()
        ) {
            bitVector.setCharAt(iter, (char) i.intValue());
            iter += 1;
        }
    }

    private void getSucc(LinkedList<String> succ, Transfer.T transfer) {
        if (transfer instanceof If) {
            succ.add(((If) transfer).truee.toString());
            succ.add(((If) transfer).falsee.toString());
        } else if (transfer instanceof Goto) {
            succ.add(((Goto) transfer).label.toString());
        }
    }

    private void getStmLiveIn(Stm.T curStm) {
        if (stmLiveIn.get(curStm) == null) {
            stmLiveIn.put(curStm, new HashSet<>());
        }
        for (String var : stmGen.get(curStm)
        ) {
            stmLiveIn.get(curStm).add(var);
        }
        for (String var : stmLiveOut.get(curStm)
        ) {
            if (!stmKill.get(curStm).contains(var)) {
                stmLiveIn.get(curStm).add(var);
            }
        }
    }

    private void getTransferLiveIn(Transfer.T transfer) {
        if (transferLiveIn.get(transfer) == null)
            transferLiveIn.put(transfer, new HashSet<>());
        for (String var : transferGen.get(transfer)
        ) {
            transferLiveIn.get(transfer).add(var);
        }
        for (String var : transferLiveOut.get(transfer)
        ) {
            if (!transferKill.get(transfer).contains(var))
                transferLiveIn.get(transfer).add(var);
        }
    }

    // /////////////////////////////////////////////////////
    // operand
    @Override
    public void visit(Int operand) {
        return;
    }

    @Override
    public void visit(Var operand) {
        switch (kind) {
            case StmKill:
                this.oneStmKill.add(operand.id);
                break;
            case StmGen:
                this.oneStmGen.add(operand.id);
                break;
            case TransferGen:
                this.oneTransferGen.add(operand.id);
                break;
            case TransferKill:
                this.oneTransferKill.add(operand.id);
                break;
        }
        return;
    }

    @Override
    public void visit(Operand.IntArray o) {
//todo
        o.index.accept(this);
    }

    // statements
    @Override
    public void visit(Add s) {
        this.oneStmKill.add(s.dst);
        // Invariant: accept() of operand modifies "gen"
        kind = Liveness_Kind_t.StmGen;
        s.left.accept(this);
        s.right.accept(this);
        return;
    }

    @Override
    public void visit(InvokeVirtual s) {
        this.oneStmKill.add(s.dst);
        this.oneStmGen.add(s.obj);
        kind = Liveness_Kind_t.StmGen;
        for (Operand.T arg : s.args) {
            arg.accept(this);
        }
        return;
    }

    @Override
    public void visit(Lt s) {
        this.oneStmKill.add(s.dst);
        // Invariant: accept() of operand modifies "gen"
        kind = Liveness_Kind_t.StmGen;
        s.left.accept(this);
        s.right.accept(this);
        return;
    }

    @Override
    public void visit(Move s) {
        this.oneStmKill.add(s.dst);
        // Invariant: accept() of operand modifies "gen"
        kind = Liveness_Kind_t.StmGen;
        s.src.accept(this);
        return;
    }

    @Override
    public void visit(NewObject s) {
        this.oneStmKill.add(s.dst);
        return;
    }

    @Override
    public void visit(NewIntArray m) {
        this.oneStmKill.add(m.dst);
    }

    @Override
    public void visit(Not m) {
        this.oneStmKill.add(m.dst);
        m.src.accept(this);
    }

    @Override
    public void visit(Print s) {
        kind = Liveness_Kind_t.StmGen;
        s.arg.accept(this);
        return;
    }

    @Override
    public void visit(Stm.Length s) {
        //TODO
        this.oneStmKill.add(s.dst);
    }

    @Override
    public void visit(Sub s) {
        this.oneStmKill.add(s.dst);
        // Invariant: accept() of operand modifies "gen"
        kind = Liveness_Kind_t.StmGen;
        s.left.accept(this);
        s.right.accept(this);
        return;
    }

    @Override
    public void visit(Times s) {
        this.oneStmKill.add(s.dst);
        // Invariant: accept() of operand modifies "gen"
        s.left.accept(this);
        s.right.accept(this);
        return;
    }

    // transfer
    @Override
    public void visit(If s) {
        kind = Liveness_Kind_t.TransferGen;
        // Invariant: accept() of operand modifies "gen"
        s.operand.accept(this);
        return;
    }

    @Override
    public void visit(Goto s) {
        return;
    }

    @Override
    public void visit(Return s) {
        kind = Liveness_Kind_t.TransferGen;
        // Invariant: accept() of operand modifies "gen"
        s.operand.accept(this);
        return;
    }

    @Override
    public void visit(And m) {
        this.oneStmKill.add(m.dst);
        m.left.accept(this);
        m.right.accept(this);
    }

    // type
    @Override
    public void visit(ClassType t) {
    }

    @Override
    public void visit(IntType t) {
    }

    @Override
    public void visit(IntArrayType t) {
    }

    // dec
    @Override
    public void visit(DecSingle d) {
    }

    // utility functions:
    private void calculateStmTransferGenKill(BlockSingle b) {
        for (Stm.T s : b.stms) {
            this.oneStmGen = new java.util.HashSet<>();
            this.oneStmKill = new java.util.HashSet<>();
            s.accept(this);
            this.stmGen.put(s, this.oneStmGen);
            this.stmKill.put(s, this.oneStmKill);
            if (control.Control.isTracing("liveness.step1")) {
                System.out.print("\ngen, kill for statement " + s + ":");
                s.toString();
                System.out.print("\ngen is:");
                for (String str : this.oneStmGen) {
                    System.out.print(str + ", ");
                }
                System.out.print("\nkill is:");
                for (String str : this.oneStmKill) {
                    System.out.print(str + ", ");
                }
            }
        }
        this.oneTransferGen = new java.util.HashSet<>();
        this.oneTransferKill = new java.util.HashSet<>();
        b.transfer.accept(this);
        this.transferGen.put(b.transfer, this.oneTransferGen);
        this.transferKill.put(b.transfer, this.oneTransferGen);
        if (control.Control.isTracing("liveness.step1")) {
            System.out.print("\ngen, kill for transfer " + b.transfer + ":");
            b.toString();
            System.out.print("\ngen is:");
            for (String str : this.oneTransferGen) {
                System.out.print(str + ", ");
            }
            System.out.println("\nkill is:");
            for (String str : this.oneTransferKill) {
                System.out.print(str + ", ");
            }
        }
        return;
    }

    // block
    @Override
    public void visit(BlockSingle b) {
        switch (this.kind) {
            case StmGenKill:
                calculateStmTransferGenKill(b);
                break;
            default:
                // Your code here:
                return;
        }
    }

    // method
    @Override
    public void visit(MethodSingle m) {
        // Four steps:
        // Step 1: calculate the "gen" and "kill" sets for each
        // statement and transfer
        for (Block.T block : m.blocks) {
            kind = Liveness_Kind_t.StmGenKill;
            block.accept(this);
        }

        // Step 2: calculate the "gen" and "kill" sets for each block.
        // For this, you should visit statements and transfers in a
        // block in a reverse order.
        // Your code here:


        //construct block kill and gen.
        for (Block.T block : m.blocks) {
            HashSet<String> blockKillVar = new HashSet<>();
            HashSet<String> blockGenVar = new HashSet<>();
            block = (BlockSingle) block;
            for (String var : transferGen.get(((BlockSingle) block).transfer)
            ) {
                blockGenVar.add(var);
            }

            for (int i = ((BlockSingle) block).stms.size() - 1; i >= 0; i--) {
                for (String var : stmGen.get(((BlockSingle) block).stms.get(i)))
                    if (!blockKillVar.contains(var))
                        blockGenVar.add(var);

                for (String var : stmKill.get(((BlockSingle) block).stms.get(i)))
                    blockKillVar.add(var);
            }

            blockGen.put(block, blockGenVar);
            blockKill.put(block, blockKillVar);
        }


        // Step 3: calculate the "liveIn" and "liveOut" sets for each block
        // Note that to speed up the calculation, you should first
        // calculate a reverse topo-sort order of the CFG blocks, and
        // crawl through the blocks in that order.
        // And also you should loop until a fix-point is reached.
        // Your code here:

        LinkedHashMap<Block.T, Integer> fixPointInFlag = new LinkedHashMap<>();
        LinkedHashMap<Block.T, Integer> fixPointOutFlag = new LinkedHashMap<>();
        StringBuffer bitVectorIn = new StringBuffer();
        StringBuffer bitVectorOut = new StringBuffer();
        for (Block.T block : m.blocks
        ) {
            fixPointInFlag.put(block, 0);
            fixPointOutFlag.put(block, 0);
            bitVectorIn.append(-1);
            bitVectorOut.append(-1);
            blockLiveIn.put(block, new HashSet<>());
            blockLiveOut.put(block, new HashSet<>());
        }
        while (checkFixPoint(fixPointInFlag, bitVectorIn) || checkFixPoint(fixPointOutFlag, bitVectorOut)) {
            initFixPointFlagAndBitVector(fixPointInFlag, bitVectorIn);
            initFixPointFlagAndBitVector(fixPointOutFlag, bitVectorOut);

            for (Block.T block : m.blocks
            ) {
                //rebuild in set
                for (String var : blockGen.get(block)
                ) {
                    blockLiveIn.get(block).add(var);
                }

                for (String var : blockLiveOut.get(block)
                ) {
                    if (!blockKill.get(block).contains(var)) {
                        blockLiveIn.get(block).add(var);
                    }
                }
                fixPointInFlag.put(block, blockLiveIn.size());

                //collect succ info.
                Transfer.T transfer = ((BlockSingle) block).transfer;
                LinkedList<String> succ = new LinkedList<>();
                getSucc(succ, transfer);

                //rebuild out set
                for (Block.T mayBeIn : m.blocks
                ) {
                    if (succ.contains(((BlockSingle) mayBeIn).label.toString())) {
                        for (String var : blockLiveIn.get(mayBeIn)
                        ) {
                            blockLiveOut.get(block).add(var);
                        }
                    }
                    fixPointOutFlag.put(block, blockLiveOut.size());
                }
            }

        }


        // Step 4: calculate the "liveIn" and "liveOut" sets for each
        // statement and transfer
        // Your code here:

        for (int i = m.blocks.size() - 1; i >= 0; i--) {
            BlockSingle block = (BlockSingle) m.blocks.get(i);
            HashSet<String> preIn = null;
            Stm.T curStm;
            int offset = 0;
            if (block.stms.size() > 0) {
                Transfer.T transfer = block.transfer;
                LinkedList<String> succ = new LinkedList<>();
                getSucc(succ, transfer);
                if (transfer instanceof If || transfer instanceof Return) {
                    transferLiveOut.put(transfer, new HashSet<>());
                    transferLiveIn.put(transfer, new HashSet<>());
                    for (Block.T maySuccBlock : m.blocks
                    ) {
                        if (succ.contains(((BlockSingle) maySuccBlock).label.toString())) {
                            for (String var : blockLiveIn.get(maySuccBlock)
                            )
                                transferLiveOut.get(transfer).add(var);
                        }
                    }
                    getTransferLiveIn(transfer);
                    preIn = transferLiveIn.get(transfer);
                } else {
                    curStm = block.stms.get(block.stms.size() - 1);
                    stmLiveIn.put(curStm, new HashSet<>());
                    stmLiveOut.put(curStm, new HashSet<>());
                    for (Block.T maySuccBlock : m.blocks
                    ) {
                        if (succ.contains(((BlockSingle) maySuccBlock).label.toString())) {
                            for (String var : blockLiveIn.get(maySuccBlock)
                            )
                                stmLiveOut.get(curStm).add(var);
                        }
                    }
                    getStmLiveIn(curStm);
                    preIn = stmLiveIn.get(curStm);
                    offset = 1;
                }


                for (int j = block.stms.size() - 1 - offset; j >= 0; j--) {
                    curStm = block.stms.get(j);
                    stmLiveOut.put(curStm, preIn);
                    getStmLiveIn(curStm);
                    preIn = stmLiveIn.get(curStm);
                }
            }
        }
    }

    @Override
    public void visit(MainMethodSingle m) {
        // Four steps:
        // Step 1: calculate the "gen" and "kill" sets for each
        // statement and transfer
        for (Block.T block : m.blocks) {
            kind = Liveness_Kind_t.BlockGenKill;
            block.accept(this);
        }

        // Step 2: calculate the "gen" and "kill" sets for each block.
        // For this, you should visit statements and transfers in a
        // block in a reverse order.
        // Your code here:

        // Step 3: calculate the "liveIn" and "liveOut" sets for each block
        // Note that to speed up the calculation, you should first
        // calculate a reverse topo-sort order of the CFG blocks, and
        // crawl through the blocks in that order.
        // And also you should loop until a fix-point is reached.
        // Your code here:

        // Step 4: calculate the "liveIn" and "liveOut" sets for each
        // statement and transfer
        // Your code here:
    }

    // vtables
    @Override
    public void visit(VtableSingle v) {
    }

    // class
    @Override
    public void visit(ClassSingle c) {
    }

    // program
    @Override
    public void visit(ProgramSingle p) {
        p.mainMethod.accept(this);
        for (Method.T mth : p.methods) {
            mth.accept(this);
        }

        Set<Stm.T> keyset = stmLiveOut.keySet();
        for (Stm.T stm : keyset) {
            System.out.println(stm);
            System.out.println("liveout");
            for (String var : stmLiveOut.get(stm)
            ) {
                System.out.println(var);
            }
            System.out.println("livein");
            for (String var : stmLiveIn.get(stm)
            ) {
                System.out.println(var);
            }
            System.out.println("================");
        }
        System.out.println("+++++++++++++++");
        Set<Transfer.T> transferKeyset = transferLiveIn.keySet();
        for (Transfer.T transfer : transferKeyset
        ) {
            System.out.println(transfer);
            System.out.println("liveout");
            for (String var :
                    transferLiveOut.get(transfer)) {
                System.out.println(var);
            }
            System.out.println("liveout");
            for (String var :
                    transferLiveIn.get(transfer)) {
                System.out.println(var);
            }
            System.out.println("================");
        }
        return;
    }

}
