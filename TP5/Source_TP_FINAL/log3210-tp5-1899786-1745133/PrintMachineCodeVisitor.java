package analyzer.visitors;

import analyzer.ast.*;
//import com.sun.javafx.geom.Edge;
//import com.sun.org.apache.bcel.internal.generic.RET;
//import com.sun.org.apache.xpath.internal.operations.Bool;

import javax.crypto.Mac;
import java.awt.image.ColorModel;
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

            CODE.get(CODE.size() - 1).Life_OUT.add("@" + ((ASTIdentifier) node.jjtGetChild(i)).getValue());

            //Selon moi (Roman), c'est ici qu'il faut faire les "ST"
            for(int j = 0; j < MODIFIED.size(); j++){
                if(RETURNED.get(i).equals(MODIFIED.get(j))){
                    List<String> codeToPass = new ArrayList<String>();
                    codeToPass.add("ST");
                    codeToPass.add(RETURNED.get(i).substring(1));
                    codeToPass.add(RETURNED.get(i));

                    MachLine machineLine = new MachLine(codeToPass);
                    CODE.add(machineLine);
                }
            }

        }

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
        if(!isDivisionByOne(OP.get(op), assigned, left, right) && !isMultiplicationByOne(OP.get(op), assigned, left, right)){

            addLoadToCODE(loadedLeft, left);
            addLoadToCODE(loadedRight, right);

            LOADED.add(assigned);
            MODIFIED.add(assigned);

            // add the operation
            addToCODE(OP.get(op), assigned, left, right);
        }

        return null;
    }

    public boolean isDivisionByOne(String op, String assigned, String left, String right){
        if(op.equals("DIV") && right.equals("#1") && assigned.equals(left)){
            return true;
        }
        return false;
    }

    public boolean isMultiplicationByOne(String op, String assigned, String left, String right){
        if(op.equals("MUL") && right.equals("#1") && assigned.equals(left)){
            return true;
        }
        else if(op.equals("MUL") && left    .equals("#1") && assigned.equals(right)){
            return true;
        }
        return false;
    }


    //variable is for example @a. so we pass left or right as variable
    public void addLoadToCODE(boolean isLoaded, String variable){
        if(!isLoaded && variable.charAt(0) != '#'){
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

    @Override
    public Object visit(ASTAssignUnaryStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left     = (String) node.jjtGetChild(1).jjtAccept(this, null);

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

        return null;
    }



    @Override
    public Object visit(ASTAssignDirectStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left     = (String) node.jjtGetChild(1).jjtAccept(this, null);

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
        if(!assigned.equals(left)){ //réduction de code. si a = a, on le skip
            addLoadToCODE(loadedLeft, left);


            LOADED.add(assigned);
            MODIFIED.add(assigned);

            // add the operation
            addToCODE("ADD", assigned, "#0", left);
        }

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

                buff += k + ":";

                buff += "[";
                for (int i = nextuse.get(k).size()-1; i >= 0; i--) {
                    buff += nextuse.get(k).get(i);
                    if(nextuse.get(k).size() > 1 && i != 0){
                        buff += ", ";
                    }
                }
                buff += "]";


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
            buff += "// Life_IN  : " +  set_ordered(Life_IN).toString() +"\n";
            buff += "// Life_OUT : " +  set_ordered(Life_OUT).toString() +"\n";
            buff += "// Next_IN  : " +  Next_IN.toString() +"\n";
            buff += "// Next_OUT : " +  Next_OUT.toString() +"\n";
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

                        node.Life_OUT.add(inSuccNode);

                }
            }

            // OLD_IN = IN[node];
            HashSet<String> OLD_IN = node.Life_IN;

            //temp = OUT[node] - DEF[node]
            HashSet temp = (HashSet)node.Life_OUT.clone();

            temp.removeAll(node.DEF);
            temp.addAll(node.REF);
            node.Life_IN = (HashSet<String>) temp.clone();

            if(!node.Life_IN.equals(OLD_IN)) {
                for (Integer predNode : node.PRED){
                    MachLine line = CODE.get(predNode);
                    workList.push(line);
                }
            }

            if (node.SUCC.isEmpty()) {
                RETURNED.forEach(value -> {
                    if (!MODIFIED.contains(value)) {
                        node.Life_IN.add(value);
                        node.Life_OUT.add(value);
                    }
                });
            }


        }
    }

    private void compute_NextUse() {
        // TODO: Implement NextUse algorithm on the CODE array (for machine code)

        Stack<MachLine> workList = new Stack<>();

        //on ajoute les stop nodes a la worklist
        workList.push(CODE.get(CODE.size() - 1));

        while(!workList.isEmpty()){
            // node = workList.pop();
            MachLine node = workList.pop();

            for(Integer succNode : node.SUCC){
                // OUT[node] = OUT[node] union IN[succNode];
                MachLine currentSucc = CODE.get(succNode);
                for (Map.Entry<String, ArrayList<Integer>> entry : currentSucc.Next_IN.nextuse.entrySet()) {
                    for (Integer value : entry.getValue()) {
                        node.Next_OUT.add(entry.getKey(), value);
                    }
                }
            }

            // OLD_IN = IN[node];
            NextUse OLD_IN = (NextUse) node.Next_IN.clone();

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

        }

    }

    public void compute_machineCode() {
        // TODO: Implement machine code with graph coloring for register assignation (REG is the register limitation)
        //       The pointers (ex: "@a") here should be replaced by registers (ex: R0) respecting the coloring algorithm
        //       described in the TP requirements.

        boolean grapheColoriee = false;
        HashMap<String, Integer> colorMap = new HashMap<>();    //node, color

        HashMap<String, ArrayList<String>> grapheInterferance;
        Stack<String> nodesToStack;

        while(!grapheColoriee) {
            //géneration du graphe d'interferance  (partie 4)
            grapheInterferance = new HashMap<>();
            generateGrapheInterferance(grapheInterferance);

            //coloration du graphe (partie 5)
            nodesToStack  = new Stack<>();
            colorMap = new HashMap<>();
            HashMap<String, ArrayList<String>> grapheInterferance2 = new HashMap<>((HashMap<String, ArrayList<String>>) grapheInterferance.clone());
            grapheColoriee = colorGrapheInterferance(grapheInterferance, grapheInterferance2, colorMap, nodesToStack);
        }

        //itérer dans le CODE et changer les pointeurs ex : @a par ses registres
        for (MachLine line : CODE) {

            for (int i = 0; i < line.line.size(); i++) {

                if (line.line.get(i).charAt(0) == '@') {
                    String registerNumber = "R" + colorMap.get(line.line.get(i));
                    line.line.set(i, registerNumber);
                }
            }

        }

    }

    public boolean colorGrapheInterferance(HashMap<String, ArrayList<String>> grapheInterferance, HashMap<String, ArrayList<String>> grapheInterferance2,  HashMap<String, Integer> colorMap, Stack<String> nodesToStack) {
        String nodeToStack = "";

        while (!grapheInterferance2.isEmpty()) {
            nodeToStack = getNodeToStack(grapheInterferance2);

            if(nodeToStack == "") {
                spill(grapheInterferance2);
                compute_LifeVar();
                compute_NextUse();
                return false;
            }

            grapheInterferance2.remove(nodeToStack);

            for (String node : set_ordered(grapheInterferance2.keySet())) {
                grapheInterferance2.get(node).remove(nodeToStack);
            }

            nodesToStack.push(nodeToStack);
        }

        while(!nodesToStack.isEmpty()) {

            nodeToStack = nodesToStack.pop();
            grapheInterferance2.put(nodeToStack, new ArrayList<>());
            ArrayList<String> voisins = grapheInterferance.get(nodeToStack);

            for (String voisin : voisins) {
                if (grapheInterferance2.containsKey(voisin)) {
                    grapheInterferance2.get(nodeToStack).add(voisin);
                    grapheInterferance2.get(voisin).add(nodeToStack);
                }
            }

            ArrayList<String> voisinsActuels = grapheInterferance2.get(nodeToStack);
            int color = 0;
            HashSet<Integer> voisinsCouleurs = new HashSet<>();

            for (String voisin : voisinsActuels) {
                voisinsCouleurs.add(colorMap.get(voisin));
            }

            while (voisinsCouleurs.contains(color)) {
                color++;
            }

            colorMap.put(nodeToStack, color);
        }
        return true;
    }


    public List<String> set_ordered(Set<String> s) {
        // function given to order a set in alphabetic order TODO: use it! or redo-it yourself
        List<String> list = new ArrayList<String>(s);
        Collections.sort(list);
        return list;
    }

    // TODO: add any class you judge necessary, and explain them in the report. GOOD LUCK!

    public void generateGrapheInterferance(HashMap<String, ArrayList<String>> grapheInterferance) {
        for (MachLine line: CODE) {

            for (String k : set_ordered(line.Next_OUT.nextuse.keySet())) {
                if(!grapheInterferance.keySet().contains(k)) {
                    grapheInterferance.put(k, new ArrayList<>());
                }

                for (String key : set_ordered(line.Next_OUT.nextuse.keySet())) {
                    if(!key.equals(k) && !grapheInterferance.get(k).contains(key)) {
                        grapheInterferance.get(k).add(key);
                    }
                }
            }

        }
    }

    public String getNodeToStack(HashMap<String, ArrayList<String>> grapheInterferance) {
        Integer maxVoisins = -1;
        String nodeToStack = "";

        for (String node : set_ordered(grapheInterferance.keySet())) {

            Integer voisinsSize = grapheInterferance.get(node).size();

            if(voisinsSize < REG && voisinsSize > maxVoisins) {
                maxVoisins = voisinsSize;
                nodeToStack = node;
            }
        }

        return nodeToStack;
    }

    public HashSet<String> spilledNodes = new HashSet<>();

    public void spill(HashMap<String, ArrayList<String>> grapheInterferance2) {

        String nodeToSpill = "";
        int voisinMax = 0;

        for (String node : set_ordered(grapheInterferance2.keySet())) {

            int nbVoisin = grapheInterferance2.get(node).size();

            if(nbVoisin > voisinMax && !spilledNodes.contains(node)) {
                voisinMax = nbVoisin;
                nodeToSpill = node;
            }
        }

        spilledNodes.add(nodeToSpill);

        Integer first = 0;
        Integer nbLine = 0;
        boolean trouvee = false;
        while (!trouvee) {
            String operation = CODE.get(nbLine).line.get(0);
            if(operation != "ST" && operation != "LD") {
                String assigned = CODE.get(nbLine).line.get(1);
                String left     = CODE.get(nbLine).line.get(2);
                String right    = CODE.get(nbLine).line.get(3);

                if(nodeToSpill.equals(assigned) || nodeToSpill.equals(left) || nodeToSpill.equals(right)) {
                    first = nbLine;
                    trouvee = true;
                }
            }
            nbLine++;
        }

        String assigned = CODE.get(first).line.get(0);

        if(assigned.equals(nodeToSpill)) {
            CODE.add(first + 1, getSTLine(nodeToSpill));
            compute_LifeVar();
            compute_NextUse();
        }


        if(CODE.get(first).Next_OUT.nextuse.containsKey(nodeToSpill)) {

            int nbNextUseLine = CODE.get(first).Next_OUT.nextuse.get(nodeToSpill).get(0);
            CODE.add(nbNextUseLine, getLoadLine(nodeToSpill + "!", nodeToSpill));

            for (int i = nbNextUseLine; i < CODE.size() - 1; i++) {

                String operation = CODE.get(i).line.get(0);
                String assign    = CODE.get(i).line.get(1);
                String left      = CODE.get(i).line.get(2);

                if(assign.equals(nodeToSpill)) {
                    CODE.get(i).line.set(1, nodeToSpill+"!");
                }

                if(left.equals(nodeToSpill)) {
                    CODE.get(i).line.set(2, nodeToSpill+"!");
                }

                if (!operation.equals("LD") && !operation.equals("ST")) {
                    String right = CODE.get(i).line.get(3);

                    if (right.equals(nodeToSpill)) {
                        CODE.get(i).line.set(3, nodeToSpill + "!");
                    }
                }

            }
        }

        updateCODE();

    }

    public MachLine getSTLine(String variable) {
        List<String> codeToPassBeforeOP = new ArrayList<>();
        codeToPassBeforeOP.add("ST");
        codeToPassBeforeOP.add(variable.substring(1));
        codeToPassBeforeOP.add(variable);
        return new MachLine(codeToPassBeforeOP);
    }

    public MachLine getLoadLine(String left, String right) {
        List<String> codeToPassBeforeOP = new ArrayList<>();
        codeToPassBeforeOP.add("LD");
        codeToPassBeforeOP.add(left);
        codeToPassBeforeOP.add(right.substring(1));
        return new MachLine(codeToPassBeforeOP);
    }

    public void updateCODE() {

        for (int i = 0; i < CODE.size(); i++) {
            CODE.get(i).PRED.clear();

            if(i > 0) {
                CODE.get(i).PRED.add(i - 1);
            }

            CODE.get(i).SUCC.clear();
            if(i < CODE.size() - 1) {
                CODE.get(i).SUCC.add(i + 1);
            }

            List<String> line = CODE.get(i).line;

            CODE.get(i).DEF.clear();
            CODE.get(i).DEF.add(line.get(1));

            CODE.get(i).REF.clear();
            for (int j = 2; j < line.size(); j++) {
                if (line.get(j).charAt(0) == '@')
                    CODE.get(i).REF.add(line.get(j));
            }

        }

    }
}
