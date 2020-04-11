package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Stack;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;
import java.util.*;


/**
 * Created: 19-02-15
 * Last Changed: 19-11-14
 * Author: Félix Brunet & Doriane Olewicki
 *
 * Description: Ce visiteur explore l'AST et génère un code intermédiaire.
 */

public class LifeVariablesVisitor implements ParserVisitor {

    //le m_writer est un Output_Stream connecter au fichier "result". c'est donc ce qui permet de print dans les fichiers
    //le code généré.
    private /*final*/ PrintWriter m_writer;

    public LifeVariablesVisitor(PrintWriter writer) {
        m_writer = writer;
    }
    public HashMap<String, StepStatus> allSteps = new HashMap<>();

    private int step = 0;
    private HashSet<String> previous_step = new HashSet<>();

    /*
    génère une nouvelle variable temporaire qu'il est possible de print
    À noté qu'il serait possible de rentrer en conflit avec un nom de variable définit dans le programme.
    Par simplicité, dans ce tp, nous ne concidérerons pas cette possibilité, mais il faudrait un générateur de nom de
    variable beaucoup plus robuste dans un vrai compilateur.
     */
    private String genStep() {
        return "_step" + step++;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data)  {
        //HashSet<String> previous_step = new HashSet<>();
        node.childrenAccept(this, data);
        compute_IN_OUT();
        for (int i = 0; i < step; i++) {
            m_writer.write("===== STEP " + i + " ===== \n" + allSteps.get("_step" + i).toString());
        }
        return null;
    }

    @Override
    public Object visit(ASTDeclaration node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        //HashSet<String> previous_step = (HashSet<String>) data;

        String step = genStep();
        StepStatus s = new StepStatus();
        allSteps.put(step, s);

        for (String e : previous_step) {
            allSteps.get(step).PRED.add(e);
            allSteps.get(e).SUCC.add(step);
        }


        // Update previous step to current
        previous_step = new HashSet<>();
        previous_step.add(step);

        node.childrenAccept(this, previous_step);
        return previous_step;
    }

    /*
    le If Stmt doit vérifier s'il à trois enfants pour savoir s'il s'agit d'un "if-then" ou d'un "if-then-else".
     */
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        //Expression
        node.jjtGetChild(0).jjtAccept(this,data);
        HashSet<String> mem_previous_step = (HashSet<String>) previous_step.clone();
        HashSet<String> if_previous_step;
        if (node.jjtGetNumChildren() == 2) {
            if_previous_step = (HashSet<String>) previous_step.clone(); // if no else
        }
        else {
            if_previous_step = new HashSet<>(); // if there is an else
        }
        for(int i=1; i < node.jjtGetNumChildren(); i++ ){
            //reset previous for each block
            previous_step = (HashSet<String>) mem_previous_step.clone();
            node.jjtGetChild(i).jjtAccept(this,data);
            for (String e : previous_step) {
                if_previous_step.add(e);
            }
        }

        previous_step = (HashSet<String>) if_previous_step.clone();
        //node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {

        //le succ de la permiere etape et le pred de la deuxieme etape sont deja ajoute par le ASTStmt
        node.jjtGetChild(0).jjtAccept(this, data);

        HashSet<String> mem_previous_step = (HashSet<String>) previous_step.clone();
        Iterator<String> itStart = previous_step.iterator();
        String startStep = itStart.next();

        node.jjtGetChild(1).jjtAccept(this, data);

        Iterator<String> itEnd = previous_step.iterator();
        String endStep = itEnd.next();
        // le pred de la derniere etape du while est la premiere etape
        allSteps.get(startStep).PRED.add(endStep);

        // le succ de la derniere etape du while est la premiere etape
        allSteps.get(endStep).SUCC.add(startStep);

        previous_step = (HashSet<String>) mem_previous_step.clone();

        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {      //toujours 2 childs
        if(data!=null) {
            allSteps.get(data.toString().substring(1,7)).DEF.add((String) node.jjtGetChild(0).jjtAccept(this, null));
            node.jjtGetChild(1).jjtAccept(this, data);
        }
        return null;
    }


    //Il n'y a probablement rien à faire ici
    @Override
    public Object visit(ASTExpr node, Object data){ //toujours 1 child
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {    //des fois 2 children, mais les 2 sont COMP EXPR
        getREF(node, data);
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) { //des fois 2 children, mais les 2 sont ADD EXPR
        getREF(node, data);
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) { //des fois 2 children, mais les 2 sont MUL EXPR
        getREF(node, data);
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {  //rarement 2 children, les 2 UNA EXPR
        getREF(node, data);
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) { //toujours 1 child
        if (node.jjtGetChild(0).jjtAccept(this, data) != null) {
            allSteps.get("_step" + (step - 1)).REF.add((String) node.jjtGetChild(0).jjtAccept(this, data));
        }
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTNotExpr node, Object data) { //toujours 1 child
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTGenValue node, Object data) { //toujours 1 child
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) { //toujours 0 child //#9
        node.childrenAccept(this, data);
        return node.getValue();
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    //jamais appellé//////////////////////////////////////////////////
    @Override
    public Object visit(ASTBoolValue node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTSwitchStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTCaseStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTDefaultStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }
    //FIN de jamais appellé//////////////////////////////////////////////////


    //utile surtout pour envoyé de l'informations au enfant des expressions logiques.
    private class StepStatus {
        public HashSet<String> REF = new HashSet<String>();
        public HashSet<String> DEF = new HashSet<String>();
        public HashSet<String> IN  = new HashSet<String>();
        public HashSet<String> OUT = new HashSet<String>();

        public HashSet<String> SUCC  = new HashSet<String>();
        public HashSet<String> PRED  = new HashSet<String>();

        public String toString() {
            String buff = "";
            buff += "REF : " + set_ordered(REF) +"\n";
            buff += "DEF : " + set_ordered(DEF) +"\n";
            buff += "IN  : " + set_ordered(IN) +"\n";
            buff += "OUT : " + set_ordered(OUT) +"\n";

            buff += "SUCC: " + set_ordered(SUCC) +"\n";
            buff += "PRED: " + set_ordered(PRED) +"\n";
            buff += "\n";
            return buff;
        }

        public String set_ordered(HashSet<String> s) {
            List<String> list = new ArrayList<String>(s);
            Collections.sort(list);

            return list.toString();
        }
    }

    private void compute_IN_OUT() {
        Iterator it = allSteps.entrySet().iterator();
        Stack<StepStatus> workList = new Stack<>();

        //on ajoute les stop nodes a la worklist
        while(it.hasNext()){
            Map.Entry pair = (Map.Entry)it.next();
            if(((StepStatus) pair.getValue()).SUCC.isEmpty()) {
                //System.out.println(pair.getKey() + " :\n " + ((StepStatus) pair.getValue()));
                workList.push((StepStatus) pair.getValue());
            }
        }

        while(!workList.isEmpty()){
            // node = workList.pop();
            StepStatus node = (StepStatus) workList.pop();

            for(String succNode : node.SUCC){
                // OUT[node] = OUT[node] union IN[succNode];
                for (String inSuccNode : allSteps.get(succNode).IN){
                    if(!node.OUT.contains(inSuccNode)) { //not necessary
                        node.OUT.add(inSuccNode);
                    }
                }
            }

            // OLD_IN = IN[node];
            HashSet<String> OLD_IN = node.IN;

            //IN[node] = (OUT[node] − DEF[node]) union REF[node];

            //temp = OUT[node] - DEF[node]
            HashSet temp = (HashSet)node.OUT.clone();
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

            node.IN = temp;

            // if (node.IN != OLD_IN)
            boolean hashSetEqual = true;
            for(String current: node.IN){
                if(!OLD_IN.contains(current)){
                    hashSetEqual = false;
                }
            }

            // if (node.IN != OLD_IN)
            if(!hashSetEqual){
                for(String predNode: node.PRED){
                    workList.push(allSteps.get(predNode));
                }
            }

        }
    }

    private void getREF(Node node, Object data) {
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            String ref = (String)node.jjtGetChild(i).jjtAccept(this, data);
            if (ref != null) {
                allSteps.get("_step" + (step - 1)).REF.add(ref);
            }

        }
    }
}
