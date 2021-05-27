package elaborator;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;

import ast.Ast;
import ast.Ast.Dec;
import ast.Ast.Type;
import util.Todo;

public class MethodTable {
    private java.util.Hashtable<String, Type.T> table;

    private Hashtable<String, Boolean> decUsed;

    public MethodTable() {
        this.table = new java.util.Hashtable<String, Type.T>();
        this.decUsed = new Hashtable<>();
    }

    public void clearTable() {
        table.clear();
    }

    // Duplication is not allowed
    public void put(LinkedList<Dec.T> formals,
                    LinkedList<Dec.T> locals) {
        for (Dec.T dec : formals) {
            Dec.DecSingle decc = (Dec.DecSingle) dec;
            if (this.table.get(decc.id) != null) {
                System.out.println("duplicated parameter: " + decc.id);
                System.exit(1);
            }
            this.table.put(decc.id, decc.type);
        }

        for (Dec.T dec : locals) {
            Dec.DecSingle decc = (Dec.DecSingle) dec;
            if (this.table.get(decc.id) != null) {
                System.out.println("duplicated variable: " + decc.id);
                System.exit(1);
            }
            this.table.put(decc.id, decc.type);
            this.decUsed.put(decc.id, false);
        }

    }

    public void setDecTrue(String id) {
        decUsed.put(id, true);
    }

    public Hashtable<String, Boolean> getDecUsed() {
        return decUsed;
    }


    // return null for non-existing keys
    public Type.T get(String id) {
        return this.table.get(id);
    }

    public void dump() {
//    new Todo();
        System.out.println("========Method Table Info========");
        Set<String> keys = this.table.keySet();
        for (String key : keys
        ) {
            System.out.println(key + "|->" + this.table.get(key));
        }
        System.out.println("----------------------------");
    }

    @Override
    public String toString() {
        return this.table.toString();
    }
}
