package analyzer.visitors;

import analyzer.ast.*;


import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Stack;
import java.util.List;
import java.util.Collections;
import java.util.function.Predicate;


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
        node.childrenAccept(this, data);
        //TODO
        /*node.jjtGetChild(0).jjtAccept(this,data);
        HashSet<String> mem_previous_step = (HashSet<String>) previous_step.clone();
        HashSet<String> if_previous_step;
        if_previous_step = (HashSet<String>) previous_step.clone(); // if no else

        for(int i=1; i < node.jjtGetNumChildren(); i++ ){
            //reset previous for each block
            previous_step = (HashSet<String>) mem_previous_step.clone();
            node.jjtGetChild(i).jjtAccept(this,data);
            for (String e : previous_step) {
                if_previous_step.add(e);
            }
        }

        previous_step = (HashSet<String>) if_previous_step.clone();*/
        return null;
    }


    @Override
    public Object visit(ASTAssignStmt node, Object data) {      //toujours 2 childs
        node.childrenAccept(this, data);
        allSteps.get("_step" + (step - 1)).DEF.add(((ASTIdentifier) node.jjtGetChild(0)).getValue());
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            String reference = (String) node.jjtGetChild(i).jjtAccept(this, data);
            if (reference != null) {
                allSteps.get("_step" + (step - 1)).REF.add(reference);
            }
        }
        //System.out.println(data);
        //System.out.println(node.jjtGetNumChildren());
        //System.out.println(node.jjtGetChild(0).toString());     //identifier (dernier)
        //System.out.println(node.jjtGetChild(1).toString());     //Expr    (premier)
        //allSteps.get(data.toString().substring(1,7)).DEF.add(node.jjtGetChild(0).toString());
        //allSteps.get(data.toString().substring(1,7)).DEF.add(node.jjtGetChild(1).toString());
        return null;
    }


    //Il n'y a probablement rien à faire ici
    @Override
    public Object visit(ASTExpr node, Object data){ //toujours 1 child
        //System.out.println("EXPRESSION");     //#1
        //System.out.println(node.jjtGetChild(0));
        //System.out.println(node.jjtGetNumChildren());
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {    //des fois 2 children, mais les 2 sont COMP EXPR
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            String reference = (String) node.jjtGetChild(i).jjtAccept(this, data);
            if (reference != null) {
                allSteps.get("_step" + (step - 1)).REF.add(reference);
            }
        }
        return node.jjGetChild(0).jjAccept(this, data);
        //System.out.println("BOOL EXPR");      //#2
        //System.out.println(node.jjtGetChild(0));
        //System.out.println(node.jjtGetNumChildren());
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) { //des fois 2 children, mais les 2 sont ADD EXPR
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            String reference = (String) node.jjtGetChild(i).jjtAccept(this, data);
            if (reference != null) {
                allSteps.get("_step" + (step - 1)).REF.add(reference);
            }
        }
        return node.jjGetChild(0).jjAccept(this, data);
        //System.out.println("COMP EXPR");      //#3
        //System.out.println(node.jjtGetChild(0));
        //System.out.println(node.jjtGetNumChildren());
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) { //des fois 2 children, mais les 2 sont MUL EXPR
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            String reference = (String) node.jjtGetChild(i).jjtAccept(this, data);
            if (reference != null) {
                allSteps.get("_step" + (step - 1)).REF.add(reference);
            }
        }
        return node.jjGetChild(0).jjAccept(this, data);
        //System.out.println("ADD EXPR");      //#4
        //System.out.println(node.jjtGetChild(0));
        //System.out.println(node.jjtGetNumChildren());
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {  //rarement 2 children, les 2 UNA EXPR
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            String reference = (String) node.jjtGetChild(i).jjtAccept(this, data);
            if (reference != null) {
                allSteps.get("_step" + (step - 1)).REF.add(reference);
            }
        }
        return node.jjGetChild(0).jjAccept(this, data);
        //System.out.println("MUL EXPR");      //#5
        //System.out.println(node.jjtGetChild(0));
        //System.out.println(node.jjtGetNumChildren());
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) { //toujours 1 child
        node.childrenAccept(this, data);
        //System.out.println("UNA EXPR");      //#6
        //System.out.println(node.jjtGetChild(0));
        //System.out.println(node.jjtGetNumChildren());
        return null;
    }

    @Override
    public Object visit(ASTNotExpr node, Object data) { //toujours 1 child
        node.childrenAccept(this, data);
        //System.out.println("NOT EXPR");      //#7
        //System.out.println(node.jjtGetChild(0));
        //System.out.println(node.jjtGetNumChildren());
        return null;
    }

    @Override
    public Object visit(ASTGenValue node, Object data) { //toujours 1 child
        node.childrenAccept(this, data);
        //System.out.println("GEN EXPR");      //#8
        //System.out.println(node.jjtGetChild(0));
        //System.out.println(node.jjtGetNumChildren());
        return null;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) { //toujours 0 child //#9
        node.childrenAccept(this, data);
        if(!("" + data).equals("null")){
            // System.out.println(data);
            // System.out.println(node.getValue());
            allSteps.get(data.toString().substring(1,7)).DEF.add(node.getValue());
        }
        return node.getValue();
    }

    //soit à ne rien faire, soit à mettre dans REF
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
        return;
    }
}
