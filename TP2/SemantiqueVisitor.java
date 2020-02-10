package analyzer.visitors;

import analyzer.SemantiqueError;
import analyzer.ast.*;

import javax.xml.crypto.Data;
import java.io.PrintWriter;
import java.util.HashMap;


/**
 * Created: 19-01-10
 * Last Changed: 19-01-25
 * Author: Félix Brunet
 *
 * Description: Ce visiteur explorer l'AST est renvois des erreur lorqu'une erreur sémantique est détecté.
 */

public class SemantiqueVisitor implements ParserVisitor {

    private final PrintWriter m_writer;

    public HashMap<String, VarType> SymbolTable = new HashMap<>();

    public SemantiqueVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    /*
    Le Visiteur doit lancer des erreurs lorsqu'une situation arrive.

    pour vous aider, voici le code a utiliser pour lancer les erreurs

    //utilisation d'identifiant non défini
    throw new SemantiqueError("Invalid use of undefined Identifier " + node.getValue());
    //USED ONCE

    //utilisation de nombre dans la condition d'un if ou d'un while
    throw new SemantiqueError("Invalid type in condition");
    //USED ONCE

    //assignation d'une valeur a une variable qui a déjà recu une valeur d'un autre type
    ex : a = 1; a = true;
    throw new SemantiqueError("Invalid type in assignment");
    //USED ONCE

    //expression invalide : (note, le code qui l'utilise est déjà fournis)
    throw new SemantiqueError("Invalid type in expression got " + type.toString() + " was expecting " + expectedType);
    //USED MANY TIMES DUE TO USING DataStruct.checkType();
     */

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data)  {
        node.childrenAccept(this, data);
        m_writer.print("all good");
        return null;
    }
    //Apparently the m_ops string have the number of operators in string due to addOps
    /*
    Doit enregistrer les variables avec leur type dans la table symbolique.
     */
    @Override
    public Object visit(ASTDeclaration node, Object data) {
        VarType tmp;
        if("num".equals(node.getValue())){
            tmp = VarType.Number;
        }else{
            tmp = VarType.Bool;
        }
        Object id = node.jjtGetChild(0).jjtAccept(this,node.getClass());
        if(id instanceof String){
            SymbolTable.put((String)id,tmp);
        }else{
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

    /*
    les structures conditionnelle doivent vérifier que leur expression de condition est de type booléenne
     */
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        
        if(node.jjtGetChild(0).jjtAccept(this,data).equals(VarType.Bool)){
            return null;
        }else{
            throw new SemantiqueError("Invalid type in condition");
        }
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        
        if(node.jjtGetChild(0).jjtAccept(this,data).equals(VarType.Bool)){
            return null;
        }else{
            throw new SemantiqueError("Invalid type in condition");
        }
    }

    /*
    On doit vérifier que le type de la variable est compatible avec celui de l'expression.
     */
    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        
        String key = (String)node.jjtGetChild(0).jjtAccept(this,node.getClass());
        VarType type = SymbolTable.get(key);
        if(!estCompatible(type, (VarType)node.jjtGetChild(1).jjtAccept(this, data))){
            throw new SemantiqueError("Invalid type in assignment");
        }
        return type;
        //gives the same result as
        //return null
    }

    @Override
    public Object visit(ASTExpr node, Object data){        
        //Il est normal que tous les noeuds jusqu'à expr retourne un type.
        VarType tmp = (VarType)node.jjtGetChild(0).jjtAccept(this,data);
        return tmp;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
        /*attention, ce noeud est plus complexe que les autres.
        si il n'a qu'un seul enfant, le noeud a pour type le type de son enfant.

        si il a plus d'un enfant, alors ils s'agit d'une comparaison. il a donc pour type "Bool".

        de plus, il n'est pas acceptable de faire des comparaisons de booleen avec les opérateur < > <= >=.
        les opérateurs == et != peuvent être utilisé pour les nombres et les booléens, mais il faut que le type soit le même
        des deux côté de l'égalité/l'inégalité.
        */        
        if(node.jjtGetNumChildren()==1){ return node.jjtGetChild(0).jjtAccept(this,data);}
        else{
            if(node.getValue().equals("==")|| node.getValue().equals("!=")){
                VarType expected = (VarType)node.jjtGetChild(0).jjtAccept(this,data);
                DataStruct lhs = new DataStruct(expected);
                lhs.checkType((VarType)node.jjtGetChild(1).jjtAccept(this,data));//there is always only 2 children!
                return VarType.Bool;
            }else{
                //Valid way to check too but the corrige uses the methods below this one
//                for(int i=0 ; i < node.jjtGetNumChildren(); i++){
//                    DataStruct lhs = new DataStruct((VarType)node.jjtGetChild(i).jjtAccept(this,data));
//                    lhs.checkType(VarType.Number);
//                }
//                return VarType.Bool;

                DataStruct lhs = new DataStruct((VarType)node.jjtGetChild(0).jjtAccept(this,data));
                lhs.checkType(new DataStruct((VarType)node.jjtGetChild(1).jjtAccept(this,data)),VarType.Number);
                return VarType.Bool;
            }
        }
//        return null;
    }

    /*
    opérateur binaire
    si il n'y a qu'un enfant, aucune vérification à faire.
    par exemple, un AddExpr peut retourner le type "Bool" à condition de n'avoir qu'un seul enfant.
     */
    @Override
    public Object visit(ASTAddExpr node, Object data) {
        
        if(node.jjtGetNumChildren()==1){ return node.jjtGetChild(0).jjtAccept(this,data);}
        else{
            DataStruct lhs = new DataStruct((VarType)node.jjtGetChild(0).jjtAccept(this,data));
            DataStruct rhs;
            for(int i=1 ; i < node.jjtGetNumChildren(); i++){
                rhs = new DataStruct((VarType)node.jjtGetChild(i).jjtAccept(this,data));
                lhs.checkType(rhs,VarType.Number);
                lhs = rhs;
            }
            return VarType.Number;

        }
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        
        if(node.jjtGetNumChildren()==1){ return node.jjtGetChild(0).jjtAccept(this,data);}
        else{
            for(int i=0 ; i < node.jjtGetNumChildren(); i++){
                DataStruct tmp = new DataStruct((VarType)node.jjtGetChild(i).jjtAccept(this,data));
                tmp.checkType(VarType.Number);
            }
            return VarType.Number;
        }
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        
        if(node.jjtGetNumChildren()==1){ return node.jjtGetChild(0).jjtAccept(this,data);}
        else{
            for(int i=0 ; i < node.jjtGetNumChildren(); i++){
                DataStruct tmp = new DataStruct((VarType)node.jjtGetChild(i).jjtAccept(this,data));
                tmp.checkType(VarType.Bool);
            }
            return VarType.Bool;
        }
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
    @Override
    public Object visit(ASTNotExpr node, Object data) {
        
        VarType tmp = (VarType)node.jjtGetChild(0).jjtAccept(this, data);
        if(node.getOps().size()==0){
            return tmp;
        }else {
            DataStruct compare = new DataStruct(tmp);
            compare.checkType(VarType.Bool); //throws error for us if it's not a boolean!
            return tmp;
        }
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        
//        node.childrenAccept(this, data);
        VarType tmp = (VarType)node.jjtGetChild(0).jjtAccept(this, data);
        if(node.getOps().size()==0){
            return tmp;
        }else {
            DataStruct compare = new DataStruct(tmp);
            compare.checkType(VarType.Number); //throws error for us if it's not a boolean!
            return tmp;
        }
    }

    /*
    les noeud ASTIdentifier aillant comme parent "GenValue" doivent vérifier leur type.

    Ont peut envoyé une information a un enfant avec le 2e paramètre de jjtAccept ou childrenAccept.
     */
    //Using return function here a shittons turns this program into a "synthesized" SDT
    @Override
    public Object visit(ASTGenValue node, Object data) {
        VarType tmp = (VarType)node.jjtGetChild(0).jjtAccept(this,data);
        
        return tmp;
    }


    @Override
    public Object visit(ASTBoolValue node, Object data) {
        return VarType.Bool;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) { //i suppose this guy is actually the token <ID, >
        System.out.println("ASTIdentifier : "+node.getValue());
        Object tmp = SymbolTable.get(node.getValue());
        if(data==ASTDeclaration.class){
            return node.getValue();
        } else if(tmp==null){
            throw new SemantiqueError("Invalid use of undefined Identifier " + node.getValue());
        } else if (data == ASTAssignStmt.class){
            return node.getValue();
        } else {//This is then data==ASTGenValue.class
            return tmp;
        }
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return VarType.Number;
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

        public DataStruct() {}

        public DataStruct(VarType p_type){
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

