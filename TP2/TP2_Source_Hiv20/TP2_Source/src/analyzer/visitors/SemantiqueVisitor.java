package analyzer.visitors;

import analyzer.SemantiqueError;
import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

/**
 * Created: 19-01-10
 * Last Changed: 19-01-25
 * Author: Félix Brunet
 * <p>
 * Description: Ce visiteur explorer l'AST est renvois des erreur lorqu'une erreur sémantique est détecté.
 */

public class SemantiqueVisitor implements ParserVisitor {

    private final PrintWriter m_writer;

    private HashMap<String, VarType> SymbolTable = new HashMap<>();

    public int VAR = 0;
    public int WHILE = 0;
    public int IF = 0;
    public int OP = 0;

    public SemantiqueVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    /*
    Le Visiteur doit lancer des erreurs lorsqu'un situation arrive.

    pour vous aider, voici le code a utilisé pour lancer les erreurs

    //utilisation d'identifiant non défini
    throw new SemantiqueError("Invalid use of undefined Identifier " + node.getValue());

    //utilisation de nombre dans la condition d'un if ou d'un while
    throw new SemantiqueError("Invalid type in condition");

    //assignation d'une valeur a une variable qui a déjà recu une valeur d'un autre type
    ex : a = 1; a = true;
    throw new SemantiqueError("Invalid type in assignment");

    //expression invalide : (note, le code qui l'utilise est déjà fournis)
    throw new SemantiqueError("Invalid type in expression got " + type.toString() + " was expecting " + expectedType);

     */

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        node.childrenAccept(this, data);
        m_writer.print(String.format("{VAR:%d, WHILE:%d, IF:%d, OP:%d}", this.VAR, this.WHILE, this.IF, this.OP));
        return null;
    }

    /*
    Doit enregistrer les variables avec leur type dans la table symbolique.
     */
    @Override
    public Object visit(ASTDeclaration node, Object data) {
        for (int i = 0 ; i < node.jjtGetNumChildren(); i++) {
            if (SymbolTable.containsKey(((ASTIdentifier) node.jjtGetChild(0)).getValue()))
                throw new SemantiqueError("Invalid type in assignment");

            SymbolTable.put(((ASTIdentifier) node.jjtGetChild(0)).getValue(), node.getValue().equals("num") ? VarType.Number : VarType.Bool);
        }
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    private void callChildenCond(SimpleNode node) {
        DataStruct d = new DataStruct();
        node.jjtGetChild(0).jjtAccept(this, d);
        VarType exprType = d.type;

        int numChildren = node.jjtGetNumChildren();
        for (int i = 1; i < numChildren; i++) {
            d = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, d);
        }
    }

    /*
    les structures conditionnelle doivent vérifier que leur expression de condition est de type booléenne
    On doit aussi compter les conditions dans les variables IF et WHILE
     */
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        try {
            // It's condition must be of type Bool
            node.childrenAccept(this, VarType.Bool);
        } catch (SemantiqueError e) {
            throw new SemantiqueError("Invalid type in condition");
        }
        IF++;
        return null;

        /*callChildenCond(node);
        //vérifier si booléenne
        if(node.jjtGetChild(0).jjtAccept(this,data).equals(VarType.Bool)){
            IF++;
            return null;
        }
        else {
            throw new SemantiqueError("Invalid type in condition");
        }*/


    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {

        try {
            // It's condition must be of type Bool
            node.childrenAccept(this, VarType.Bool);
        } catch (SemantiqueError e) {
            throw new SemantiqueError("Invalid type in condition");
        }
        WHILE++;
        return null;

        /*callChildenCond(node);
        //vérifier si booléenne
        if(node.jjtGetChild(0).jjtAccept(this,data).equals(VarType.Bool)){
            WHILE = WHILE + 1;
            return null;
        }
        else {
            throw new SemantiqueError("Invalid type in condition");
        }*/

    }

    /*
    On doit vérifier que le type de la variable est compatible avec celui de l'expression.
     */
    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        if (!SymbolTable.containsKey(((ASTIdentifier)node.jjtGetChild(0)).getValue()))
            throw new SemantiqueError("Invalid use of undefined Identifier " + ((ASTIdentifier)node.jjtGetChild(0)).getValue());

        node.childrenAccept(this, SymbolTable.get(((ASTIdentifier)node.jjtGetChild(0)).getValue()));
        VAR++; //pas exactement la bonne place...
        return null;

        /*String varName = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        VarType varType = SymbolTable.get(varName);

        DataStruct d = new DataStruct();
        node.jjtGetChild(1).jjtAccept(this, d);
        VarType exprType = d.type;

        if(varType != exprType){
            throw new SemantiqueError("Invalid type in assignment");
        }

        return null;*/
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        node.childrenAccept(this, data);

        //VarType var = (VarType)node.jjtGetChild(0).jjtAccept(this,data);

        return null;
    }


    @Override
    public Object visit(ASTCompExpr node, Object data) {
        if (node.jjtGetNumChildren() == 1) {
            //PASSTHROUGH to the next children
            node.childrenAccept(this, data);

        } else {
            if (data == VarType.Number)
                throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);

            if (node.getValue().equals("==") || node.getValue().equals("!=")) {
                try {
                    node.childrenAccept(this, VarType.Bool);
                    return null;
                } catch (SemantiqueError e) {
                    // PASSTHROUGH  If it's children are not both of type Bool
                    //              they can also be of type Number
                }
            }
            node.childrenAccept(this, VarType.Number);
        }
        return null;

        /*node.childrenAccept(this, data);
        if(node.jjtGetNumChildren() == 1){ return node.jjtGetChild(0).jjtAccept(this, data);}
        else{
            if(node.getValue().equals("==") || node.getValue().equals("!=")){
                for(int i = 0; i < node.jjtGetNumChildren(); i++){
                    DataStruct d = new DataStruct();
                    node.jjtGetChild(i).jjtAccept(this, d);
                    if(d.type != VarType.Bool){
                        throw new SemantiqueError("Invalid type in expression got " + d.toString() + " was expecting " + VarType.Bool);
                    }
                }
            }
            else{
                for(int i = 0; i < node.jjtGetNumChildren(); i++){
                    DataStruct d = new DataStruct();
                    node.jjtGetChild(i).jjtAccept(this, d);
                    if(d.type != VarType.Number){
                        throw new SemantiqueError("Invalid type in expression got " + d.toString() + " was expecting " + VarType.Number);

                    }
                }
            }
        }
        return null;*/
    }


    @Override
    public Object visit(ASTAddExpr node, Object data) {
        if (node.jjtGetNumChildren() > 1)
            if (data == VarType.Bool)
                throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);

        node.childrenAccept(this, data);
        return null;

        /*node.childrenAccept(this, data);
        if(node.jjtGetNumChildren()==1){ return node.jjtGetChild(0).jjtAccept(this,data);}
        else{
            for(int i = 0; i < node.jjtGetNumChildren(); i++){
                DataStruct d = new DataStruct();
                node.jjtGetChild(i).jjtAccept(this, d);
                if(d.type != VarType.Number){
                    throw new SemantiqueError("Invalid type in expression got " + d.type.toString() + " was expecting " + VarType.Number);
                }
            }
        }
        return null;*/
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        if (node.jjtGetNumChildren() > 1 && data == VarType.Bool)
            throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);

        node.childrenAccept(this, data);
        return null;

        /*node.childrenAccept(this, data);
        if(node.jjtGetNumChildren()==1){ return node.jjtGetChild(0).jjtAccept(this,data);}
        else{
            for(int i = 0; i < node.jjtGetNumChildren(); i++){
                DataStruct d = new DataStruct();
                node.jjtGetChild(i).jjtAccept(this, d);
                if(d.type != VarType.Number){
                    throw new SemantiqueError("Invalid type in expression got " + d.toString() + " was expecting " + VarType.Number);
                }
            }
        }
        return null;*/
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {

        if (node.jjtGetNumChildren() > 1 && data == VarType.Number)
            throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);

        node.childrenAccept(this, data);
        return null;

        /*node.childrenAccept(this, data);
        int numberOfChildren = node.jjtGetNumChildren();
        if(numberOfChildren > 1) {
            for (int i = 0; i < numberOfChildren; i++) {
                DataStruct d = new DataStruct();
                node.jjtGetChild(i).jjtAccept(this, d);
                if (d.type == VarType.Bool){
                    //visiter les child?
                }
                else{
                    throw new SemantiqueError("Invalid type in expression got " + d.type  + " was expecting " + VarType.Bool);
                }

            }
        }
        else{
            throw new SemantiqueError("Invalid type in expression");
        }
        return null;*/
    }

    /*
    opérateur unaire
    les opérateur unaire ont toujours un seul enfant.

    Cependant, ASTNotExpr et ASTUnaExpr ont la fonction "getOps()" qui retourne un vecteur contenant l'image (représentation str)
    de chaque token associé au noeud.

    Il est utile de vérifier la longueur de ce vecteur pour savoir si une opérande est présente.

    si il n'y a pas d'opérande, ne rien faire.
    si il y a une (ou plus) opérande, ils faut vérifier le type.

    */

    //pas sur si la NOT et Una sont bons...
    @Override
    public Object visit(ASTNotExpr node, Object data) {

        if (data == VarType.Number && !node.getOps().isEmpty())
            throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);

        node.childrenAccept(this, data);
        return null;

        /*node.childrenAccept(this, data);
        VarType tmp = (VarType)node.jjtGetChild(0).jjtAccept(this, data);
        if(node.getOps().size() == 0){
            return tmp;
        }else {
            for(int i = 0; i < node.jjtGetNumChildren(); i++){
                DataStruct d = new DataStruct();
                node.jjtGetChild(i).jjtAccept(this, d);
                if(d.type != VarType.Bool){
                    throw new SemantiqueError("Invalid type in expression got " + d.toString() + " was expecting " + VarType.Bool);
                }
            }
        }
        return null;*/
    }

    //pas sur si la NOT et Una sont bons...
    @Override
    public Object visit(ASTUnaExpr node, Object data) {

        if (data == VarType.Bool && !node.getOps().isEmpty())
            throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);

        node.childrenAccept(this, data);
        return null;

        /*node.childrenAccept(this, data);
        VarType tmp = (VarType)node.jjtGetChild(0).jjtAccept(this, data);
        if(node.getOps().size() == 0){
            return tmp;
        }else {
            for(int i = 0; i < node.jjtGetNumChildren(); i++){
                DataStruct d = new DataStruct();
                node.jjtGetChild(i).jjtAccept(this, d);
                if(d.type != VarType.Bool){
                    throw new SemantiqueError("Invalid type in expression got " + d.toString() + " was expecting " + VarType.Bool);
                }
            }
        }
        return null;*/
    }

    /*
    les noeud ASTIdentifier aillant comme parent "GenValue" doivent vérifier leur type.

    Ont peut envoyé une information a un enfant avec le 2e paramètre de jjtAccept ou childrenAccept.
     */
    @Override
    public Object visit(ASTGenValue node, Object data) {
        if (data == VarType.Bool) {
            if (node.jjtGetChild(0) instanceof ASTIntValue)
                throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);

            if (node.jjtGetChild(0) instanceof ASTIdentifier &&
                    SymbolTable.get(((ASTIdentifier)node.jjtGetChild(0)).getValue()) == VarType.Number )
                throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);
        }

        if (data == VarType.Number) {
            if (node.jjtGetChild(0) instanceof ASTBoolValue)
                throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);

            if (node.jjtGetChild(0) instanceof ASTIdentifier &&
                    SymbolTable.get(((ASTIdentifier)node.jjtGetChild(0)).getValue()) == VarType.Bool )
                throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);
        }

        node.childrenAccept(this, data);
        return null;

        /*node.childrenAccept(this, data);
        VarType tmp = (VarType)node.jjtGetChild(0).jjtAccept(this,data);
        return tmp;
        //return null;
        */
    }


    @Override
    public Object visit(ASTBoolValue node, Object data) {
        if (data == VarType.Number)
            throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);

        return null;
        /*((DataStruct) data).type = VarType.Bool;
        //return null;
        return VarType.Bool;*/
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        if (node.jjtGetParent() instanceof ASTGenValue && !SymbolTable.containsKey(node.getValue()))
            throw new SemantiqueError("Invalid use of undefined Identifier " + node.getValue());

        return null;
        /*if (node.jjtGetParent() instanceof ASTGenValue) {
            String varName = node.getValue();
            VarType varType = SymbolTable.get(varName);

            ((DataStruct) data).type = varType;
        }
        return null;*/
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        if (data == VarType.Bool)
            throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);

        return null;
        /*((DataStruct) data).type = VarType.Number;
       // return null;
        return VarType.Number;*/
    }


    //des outils pour vous simplifier la vie et vous enligner dans le travail
    public enum VarType {
        Bool,
        Number
    }

    private boolean estCompatible(VarType a, VarType b) {
        return a == b;
    }

    private class DataStruct {
        public VarType type;

        public DataStruct() {
        }

        public DataStruct(VarType p_type) {
            type = p_type;
        }

        public void checkType(VarType expectedType) {
            if(!estCompatible(type, expectedType)) {
                throw new SemantiqueError("Invalid type in expression got " + type.toString() + " was expecting " + expectedType);
            }
        }

        public void checkType(DataStruct d, VarType expectedType) {
            if(!estCompatible(type, expectedType) || !estCompatible(d.type, expectedType)) {
                throw new SemantiqueError("Invalid type in expression got " + type.toString() + " and " + d.type.toString() + " was expecting " + expectedType);
            }
        }
    }
}
