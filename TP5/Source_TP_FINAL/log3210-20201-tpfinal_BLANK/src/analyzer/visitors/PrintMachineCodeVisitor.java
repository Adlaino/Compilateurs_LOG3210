package analyzer.visitors;

import analyzer.ast.*;
//import com.sun.javafx.geom.Edge;
//import com.sun.org.apache.bcel.internal.generic.RET;
//import com.sun.org.apache.xpath.internal.operations.Bool;

import javax.crypto.Mac;
import java.io.PrintWriter;
import java.util.*;

public class PrintMachineCodeVisitor implements ParserVisitor {

    private PrintWriter m_writer = null;

    private Integer REG = 256; // default register limitation
    private ArrayList<String> RETURNED = new ArrayList<String>(); // returned variables from the return statement
    private ArrayList<MachLine> CODE   = new ArrayList<MachLine>(); // representation of the Machine Code in Machine lines (MachLine)
    private ArrayList<String> LOADED   = new ArrayList<String>(); // could be use to keep which variable/pointer are loaded/ defined while going through the intermediate code
    private ArrayList<String> MODIFIED = new ArrayList<String>(); // could be use to keep which variable/pointer are modified while going through the intermediate code

    private HashMap<String,String> OP; // map to get the operation name from it's value
    public PrintMachineCodeVisitor(PrintWriter writer) {
        m_writer = writer;

        OP = new HashMap<>();
        OP.put("+", "ADD");
        OP.put("-", "MIN");
        OP.put("*", "MUL");
        OP.put("/", "DIV");


    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        // Visiter les enfants
        node.childrenAccept(this, null);

        compute_LifeVar(); // first Life variables computation (should be recalled when machine code generation)
        compute_NextUse(); // first Next-Use computation (should be recalled when machine code generation)
        compute_machineCode(); // generate the machine code from the CODE array (the CODE array should be transformed

        for (int i = 0; i < CODE.size(); i++) // print the output
            m_writer.println(CODE.get(i));
        return null;
    }


    @Override
    public Object visit(ASTNumberRegister node, Object data) {
        REG = ((ASTIntValue) node.jjtGetChild(0)).getValue(); // get the limitation of register
        return null;
    }

    @Override
    public Object visit(ASTReturnStmt node, Object data) {

        for(int i = 0; i < node.jjtGetNumChildren(); i++) {
            RETURNED.add("@" + ((ASTIdentifier) node.jjtGetChild(i)).getValue()); // returned values (here are saved in "@*somthing*" format, you can change that if you want.

            // TODO: the returned variables should be added to the Life_OUT set of the last statement of the basic block (before the "ST" expressions in the machine code)

            CODE.get(CODE.size()-1).Life_OUT.add("@" + ((ASTIdentifier) node.jjtGetChild(i)).getValue());
            //System.out.println("Returned variable: " + "@" + ((ASTIdentifier) node.jjtGetChild(i)).getValue() + " added to the Life_OUT of: " + CODE.get(CODE.size()-1) );

            //Selon moi (Roman), c'est ici qu'il faut faire les "ST"
            for(int j = 0; j < MODIFIED.size(); j++){
                if(RETURNED.get(i).equals(MODIFIED.get(j))){
                    List<String> codeToPass = new ArrayList<String>();
                    codeToPass.add("ST");
                    codeToPass.add(RETURNED.get(i).substring(1));
                    codeToPass.add(RETURNED.get(i));

                    //System.out.println("ST " + RETURNED.get(i).substring(1) + ", " + RETURNED.get(i));

                    MachLine machineLine = new MachLine(codeToPass);
                    CODE.add(machineLine);
                }
            }

        }
        //System.out.println(RETURNED);
        //System.out.println(CODE);


        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left     = (String) node.jjtGetChild(1).jjtAccept(this, null);
        String right    = (String) node.jjtGetChild(2).jjtAccept(this, null);
        String op       = node.getOp();

        // TODO: Modify CODE to add the needed MachLine.
        //       here the type of Assignment is "assigned = left op right" and you should put pointers in the MachLine at
        //       the moment (ex: "@a")

        // if left or right do not exist before assigned, load them
        // aka if it's not in LOADED, load them
        boolean loadedLeft = false;
        boolean loadedRight = false;

        for(int i = 0; i < LOADED.size(); i++){
            if(LOADED.get(i).equals(left)){
                loadedLeft = true;
            }
            if(LOADED.get(i).equals(right)){
                loadedRight = true;
            }
        }

        //if left or right was not loaded, then load it
        addLoadToCODE(loadedLeft, left);
        addLoadToCODE(loadedRight, right);

        LOADED.add(assigned);
        MODIFIED.add(assigned);

        // add the operation
        addToCODE(OP.get(op), assigned, left, right);

        //System.out.println(LOADED);
        //System.out.println(MODIFIED);
        //System.out.println(CODE);

        return null;
    }

    //variable is for example @a. so we pass left or right as variable
    public void addLoadToCODE(boolean isLoaded, String variable){
        if(!isLoaded){
            LOADED.add(variable);
            List<String> codeToPassBeforeOP = new ArrayList<String>();
            codeToPassBeforeOP.add("LD");
            codeToPassBeforeOP.add(variable);
            codeToPassBeforeOP.add(variable.substring(1));

            MachLine machineLine = new MachLine(codeToPassBeforeOP);
            CODE.add(machineLine);
        }
    }

    public void addToCODE(String operation, String assigned, String left, String right){
        List<String> codeToPass = new ArrayList<String>();
        codeToPass.add(operation);
        codeToPass.add(assigned);
        codeToPass.add(left);
        codeToPass.add(right);

        MachLine machineLine = new MachLine(codeToPass);
        CODE.add(machineLine);
    }

    //ce cas n'existe même pas dans les tests, et quand on l'ajoute, ca fonctione pas....
    @Override
    public Object visit(ASTAssignUnaryStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        // TODO: Modify CODE to add the needed MachLine.
        //       here the type of Assignment is "assigned = - left" and you should put pointers in the MachLine at
        //       the moment (ex: "@a")

        boolean loadedLeft = false;
        for(int i = 0; i < LOADED.size(); i++){
            if(LOADED.get(i).equals(left)){
                loadedLeft = true;
            }
        }

        //if left was not loaded, then load it
        addLoadToCODE(loadedLeft, left);

        LOADED.add(assigned);
        MODIFIED.add(assigned);

        // add the operation
        addToCODE("SUB", assigned, "#0", left);

        //System.out.println(LOADED);
        //System.out.println(MODIFIED);
        //System.out.println(CODE);

        return null;
    }



    @Override
    public Object visit(ASTAssignDirectStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        // TODO: Modify CODE to add the needed MachLine.
        //       here the type of Assignment is "assigned = left" and you should put pointers in the MachLine at
        //       the moment (ex: "@a")

        boolean loadedLeft = false;
        for(int i = 0; i < LOADED.size(); i++){
            if(LOADED.get(i).equals(left)){
                loadedLeft = true;
            }
        }

        //if left was not loaded, then load it
        addLoadToCODE(loadedLeft, left);

        LOADED.add(assigned);
        MODIFIED.add(assigned);

        // add the operation
        addToCODE("ADD", assigned, "#0", left);

        //System.out.println(LOADED);
        //System.out.println(MODIFIED);
        //System.out.println(CODE);

        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        //nothing to do here
        return node.jjtGetChild(0).jjtAccept(this, null);
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        //nothing to do here
        return "#"+String.valueOf(node.getValue());
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        //nothing to do here
        return "@" + node.getValue();
    }


    private class NextUse {
        // NextUse class implementation: you can use it or redo it your way
        public HashMap<String, ArrayList<Integer>> nextuse = new HashMap<String, ArrayList<Integer>>();

        public NextUse() {}

        public NextUse(HashMap<String, ArrayList<Integer>> nextuse) {
            this.nextuse = nextuse;
        }

        public void add(String s, int i) {
            if (!nextuse.containsKey(s)) {
                nextuse.put(s, new ArrayList<Integer>());
            }
            nextuse.get(s).add(i);
        }

        public String toString() {
            String buff = "";
            boolean first = true;
            for (String k : set_ordered(nextuse.keySet())) {
                if (! first) {
                    buff +=", ";
                }
                buff += k + ":" + nextuse.get(k);
                first = false;
            }
            return buff;
        }

        public Object clone() {
            return new NextUse((HashMap<String, ArrayList<Integer>>) nextuse.clone());
        }
    }


    private class MachLine {
        List<String> line;
        public HashSet<String> REF = new HashSet<String>();
        public HashSet<String> DEF = new HashSet<String>();
        public HashSet<Integer> SUCC  = new HashSet<Integer>();
        public HashSet<Integer> PRED  = new HashSet<Integer>();
        public HashSet<String> Life_IN  = new HashSet<String>();
        public HashSet<String> Life_OUT = new HashSet<String>();

        public NextUse Next_IN  = new NextUse();
        public NextUse Next_OUT = new NextUse();

        public MachLine(List<String> s) {
            this.line = s;
            int size = CODE.size();

            // PRED, SUCC, REF, DEF already computed (cadeau)
            if (size > 0) {
                PRED.add(size-1);
                CODE.get(size-1).SUCC.add(size);
            }
            this.DEF.add(s.get(1));
            for (int i = 2; i < s.size(); i++)
                if (s.get(i).charAt(0) == '@')
                    this.REF.add(s.get(i));
        }

        public String toString() {
            String buff = "";

            // print line :
            buff += line.get(0) + " " + line.get(1);
            for (int i = 2; i < line.size(); i++)
                buff += ", " + line.get(i);
            buff +="\n";
            // you can uncomment the others set if you want to see them.
            //buff += "// REF      : " +  REF.toString() +"\n";
            //buff += "// DEF      : " +  DEF.toString() +"\n";
            //buff += "// PRED     : " +  PRED.toString() +"\n";
            //buff += "// SUCC     : " +  SUCC.toString() +"\n";
            //buff += "// Life_IN  : " +  Life_IN.toString() +"\n";
            //buff += "// Life_OUT : " +  Life_OUT.toString() +"\n";
            //buff += "// Next_IN  : " +  Next_IN.toString() +"\n";
            //buff += "// Next_OUT : " +  Next_OUT.toString() +"\n";
            return buff;
        }
    }

    private void compute_LifeVar() {
        // TODO: Implement LifeVariable algorithm on the CODE array (for machine code)

        Stack<MachLine> workList = new Stack<>();

        //on ajoute les stop nodes à la worklist
        workList.push(CODE.get(CODE.size() - 1));

        while(!workList.isEmpty()){
            // node = workList.pop();
            MachLine node = workList.pop();

            for(Integer succNode : node.SUCC){
                // OUT[node] = OUT[node] union IN[succNode];
                for (String inSuccNode : CODE.get(succNode).Life_IN){
                    if(!node.Life_OUT.contains(inSuccNode)) {
                        node.Life_OUT.add(inSuccNode);
                    }
                }
            }

            // OLD_IN = IN[node];
            HashSet<String> OLD_IN = node.Life_IN;

            //IN[node] = (OUT[node] − DEF[node]) union REF[node];

            //temp = OUT[node] - DEF[node]
            HashSet temp = (HashSet)node.Life_OUT.clone();
            for(String def : node.DEF){
                if(temp.contains(def)){
                    temp.remove(def);
                }
            }

            //IN [ node ] = temp union REF[node]
            for (String refNode : node.REF){
                if(!temp.contains(refNode)) {
                    temp.add(refNode);
                }
            }

            node.Life_IN = temp;

            // if (node.IN != OLD_IN)
            boolean hashSetEqual = true;
            for(String current: node.Life_IN){
                if(!OLD_IN.contains(current)){
                    hashSetEqual = false;
                }
            }

            // if (node.IN != OLD_IN)
            if(!hashSetEqual){
                for(Integer predNode: node.PRED){
                    workList.push(CODE.get(predNode));
                }
            }

        }
    }

    private void compute_NextUse() {
        // TODO: Implement NextUse algorithm on the CODE array (for machine code)

        //clean Next_IN et Next_OUT si nécessaire

        Stack<MachLine> workList = new Stack<>();

        //on ajoute les stop nodes a la worklist
        workList.push(CODE.get(CODE.size() - 1));

        while(!workList.isEmpty()){
            // node = workList.pop();
            MachLine node = workList.pop();

            for(Integer succNode : node.SUCC){
                // OUT[node] = OUT[node] union IN[succNode];
                //TODO: il y a une diffrénce ici
                MachLine currentSucc = CODE.get(succNode);
                for (Map.Entry<String, ArrayList<Integer>> entry : currentSucc.Next_IN.nextuse.entrySet()) {
                    for (Integer value : entry.getValue()) {
                        node.Next_OUT.add(entry.getKey(), value);
                    }
                }
            }

            // OLD_IN = IN[node];
            NextUse OLD_IN = (NextUse) node.Next_IN.clone();

            //nouvelle partie
            //TODO: il y a aussi une diffrénce ici
            for (Map.Entry<String, ArrayList<Integer>> entry : node.Next_OUT.nextuse.entrySet()) {
                if (!node.DEF.contains(entry.getKey())) {
                    for (Integer value : entry.getValue()) {
                        node.Next_IN.add(entry.getKey(), value);
                    }
                }
            }
            for (String ref : node.REF) {
                node.Next_IN.add(ref, CODE.indexOf(node));
            }
            if (!node.Next_IN.nextuse.equals(OLD_IN.nextuse)) {
                for (Integer predNode : node.PRED) {
                    workList.push(CODE.get(predNode));
                }
            }
            //TODO: jusqu'à ici
//            System.out.println("node: "+ node);
//            System.out.println(worklist);

        }

    }

    public void compute_machineCode() {
        // TODO: Implement machine code with graph coloring for register assignation (REG is the register limitation)
        //       The pointers (ex: "@a") here should be replaced by registers (ex: R0) respecting the coloring algorithm
        //       described in the TP requirements.


    }


    public List<String> set_ordered(Set<String> s) {
        // function given to order a set in alphabetic order TODO: use it! or redo-it yourself
        List<String> list = new ArrayList<String>(s);
        Collections.sort(list);
        return list;
    }

    // TODO: add any class you judge necessary, and explain them in the report. GOOD LUCK!
}
