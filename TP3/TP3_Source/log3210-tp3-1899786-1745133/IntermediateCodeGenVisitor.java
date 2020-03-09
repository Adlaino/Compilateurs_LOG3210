package analyzer.visitors;

import analyzer.ast.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.function.Predicate;


/**
 * Created: 19-02-15
 * Last Changed: 19-10-20
 * Author: Félix Brunet & Doriane Olewicki
 *
 * Description: Ce visiteur explore l'AST et génère un code intermédiaire.
 */

public class IntermediateCodeGenVisitor implements ParserVisitor {

    //le m_writer est un Output_Stream connecter au fichier "result". c'est donc ce qui permet de print dans les fichiers
    //le code généré.
    private final PrintWriter m_writer;

    public IntermediateCodeGenVisitor(PrintWriter writer) {
        m_writer = writer;
    }
    public HashMap<String, VarType> SymbolTable = new HashMap<>();

    private int id = 0;
    private int label = 0;
    /*
    génère une nouvelle variable temporaire qu'il est possible de print
    À noté qu'il serait possible de rentrer en conflit avec un nom de variable définit dans le programme.
    Par simplicité, dans ce tp, nous ne concidérerons pas cette possibilité, mais il faudrait un générateur de nom de
    variable beaucoup plus robuste dans un vrai compilateur.
     */
    private String genId() {

        return "_t" + id++;
    }

    //génère un nouveau Label qu'il est possible de print.
    private String genLabel() {

        return "_L" + label++;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data)  {
        node.childrenAccept(this, data);
        return null;
    }

    /*
    Code fournis pour remplir la table de symbole.
    Les déclarations ne sont plus utile dans le code à trois adresse.
    elle ne sont donc pas concervé.
     */
    @Override
    public Object visit(ASTDeclaration node, Object data) {
        ASTIdentifier id = (ASTIdentifier) node.jjtGetChild(0);
        VarType t;
        if(node.getValue().equals("bool")) {
            t = VarType.Bool;
        } else {
            t = VarType.Number;
        }
        SymbolTable.put(id.getValue(), t);
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
    le If Stmt doit vérifier s'il à trois enfants pour savoir s'il s'agit d'un "if-then" ou d'un "if-then-else".
     */
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        String next = genLabel();
        int n = node.jjtGetNumChildren();

        BoolLabel labels = new BoolLabel();
        labels.lTrue = null;
        labels.lFalse = n == 3 ? genLabel() : next;

        node.jjtGetChild(0).jjtAccept(this, labels);

        node.jjtGetChild(1).jjtAccept(this, data);
        if(n == 3) {
            m_writer.println(String.format("goto %s", next));
            m_writer.println(labels.lFalse);
            node.jjtGetChild(2).jjtAccept(this, next);
        }
        m_writer.println(next);
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        String next = genLabel();
        String begin = genLabel();

        BoolLabel labels = new BoolLabel();
        labels.lTrue = null;
        labels.lFalse = next;

        m_writer.println(begin);
        node.jjtGetChild(0).jjtAccept(this, labels);

        node.jjtGetChild(1).jjtAccept(this, begin);
        m_writer.println(String.format("goto %s", begin));
        m_writer.println(next);

        return null;
    }

    /*
     *  la difficulté est d'implémenter le "short-circuit" des opérations logiques combinez à l'enregistrement des
     *  valeurs booléennes dans des variables.
     *
     *  par exemple,
     *  a = b || c && !d
     *  deviens
     *  if(b)
     *      t1 = 1
     *  else if(c)
     *      if(d)
     *         t1 = 1
     *      else
     *         t1 = 0
     *  else
     *      t1 = 0
     *  a = t1
     *
     *  qui est équivalent à :
     *
     *  if b goto LTrue
     *  ifFalse c goto LFalse
     *  ifFalse d goto LTrue
     *  goto LFalse
     *  //Assign
     *  LTrue
     *  a = 1
     *  goto LEnd
     *  LFalse
     *  a = 0
     *  LEnd
     *  //End Assign
     *
     *  mais
     *
     *  a = 1 * 2 + 3
     *
     *  deviens
     *
     *  //expr
     *  t1 = 1 * 2
     *  t2 = t1 + 3
     *  //expr
     *  a = t2
     *
     *  et
     *
     *  if(b || c && !d)
     *
     *  deviens
     *
     *  //expr
     *  if b goto LTrue
     *  ifFalse c goto LFalse
     *  ifFalse d goto LTrue
     *  goto LFalse
     *  //expr
     *  //if
     *  LTrue
     *  codeS1
     *  goto lEnd
     *  LFalse
     *  codeS2
     *  LEnd
     *  //end if
     *
     *
     *  Il faut donc dès le départ vérifier dans la table de symbole le type de la variable à gauche, et généré du
     *  code différent selon ce type.
     *
     *  Pour avoir l'id de la variable de gauche de l'assignation, il peut être plus simple d'aller chercher la valeur
     *  du premier enfant sans l'accepter.
     *  De la sorte, on accept un noeud "Identifier" seulement lorsqu'on l'utilise comme référence (à droite d'une assignation)
     *  Cela simplifie le code de part et d'autre.
     *
     *  Aussi, il peut être pertinent d'extraire le code de l'assignation dans une fonction privée, parce que ce code
     *  sera utile pour les noeuds de comparaison (plus d'explication au commentaire du noeud en question.)
     *  la signature de la fonction que j'ai utilisé pour se faire est :
     *  private String generateAssignCode(Node node, String tId);
     *  ou "node" est le noeud de l'expression représentant la valeur, et tId est le nom de la variable ou assigné
     *  la valeur.
     *
     *  Il est normal (et probablement inévitable concidérant la structure de l'arbre)
     *  de généré inutilement des labels (ou des variables temporaire) qui ne sont pas utilisé ni imprimé dans le code résultant.
     */
    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        String id = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        if(SymbolTable.get(id) == VarType.Number) {
            String val = node.jjtGetChild(1).jjtAccept(this, null).toString();
            m_writer.println(String.format("%s = %s", id, val));
        } else {
            generateAssignCode(node.jjtGetChild(1), id);
        }
        return null;
    }

    private void generateAssignCode(Node node, String tId) {
        BoolLabel labels = new BoolLabel();
        labels.lTrue = genLabel();
        labels.lFalse = genLabel();
        String endLabel = genLabel();

        node.jjtAccept(this, labels);

        m_writer.println(labels.lTrue);
        m_writer.println(String.format("%s = 1", tId));
        m_writer.println(String.format("goto %s", endLabel));
        m_writer.println(labels.lFalse);
        m_writer.println(String.format("%s = 0", tId));
        m_writer.println(endLabel);
    }


    //Il n'y a probablement rien à faire ici
    @Override
    public Object visit(ASTExpr node, Object data){
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    //Expression arithmétique
    /*
    Les expressions arithmétique add et mult fonctionne exactement de la même manière. c'est pourquoi
    il est plus simple de remplir cette fonction une fois pour avoir le résultat pour les deux noeuds.

    On peut bouclé sur "ops" ou sur node.jjtGetNumChildren(),
    la taille de ops sera toujours 1 de moins que la taille de jjtGetNumChildren
     */
    public Object exprCodeGen(SimpleNode node, Object data, Vector<String> ops) {
        Object expr =  node.jjtGetChild(0).jjtAccept(this, data);

        for(int i = 1; i < node.jjtGetNumChildren(); i++) {
            String val = node.jjtGetChild(i).jjtAccept(this, data).toString();
            String temp = genId();
            m_writer.println(String.format("%s = %s %s %s", temp, expr, ops.get(i - 1), val));
            expr = temp;
        }

        return expr;
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) {
        return exprCodeGen(node, data, node.getOps());
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        return exprCodeGen(node, data, node.getOps());
    }

    //UnaExpr est presque pareil au deux précédente. la plus grosse différence est qu'il ne va pas
    //chercher un deuxième noeud enfant pour avoir une valeur puisqu'il s'agit d'une opération unaire.
    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        if(node.getOps().size() > 0) {
            String temp = genId();
            String op = node.getOps().get(0).toString();
            m_writer.println(String.format("%s = %s %s", temp, op, val));
            return temp;
        }
        return val;
    }

    //expression logique

    /*

    Rappel, dans le langague, le OU et le ET on la même priorité, et sont associatif à droite par défaut.
    ainsi :
    "a = a || || a2 || b && c || d" est interprété comme "a = a || a2 || (b && (c || d))"
     */
    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        BoolLabel labels = data != null ? (BoolLabel) data : null;
        String tempLabel = null;

        for(int i = 0; i < node.getOps().size(); i++) {
            BoolLabel label = new BoolLabel();
            if(node.getOps().get(i).equals("||")) {
                if(labels.lTrue == null) {
                    if(tempLabel == null) {
                        tempLabel = genLabel();
                    }
                    label.lTrue = tempLabel;
                } else {
                    label.lTrue = labels.lTrue;
                }
                label.lFalse = null;
            } else {
                label.lTrue = null;
                if(labels.lFalse == null) {
                    if(tempLabel == null) {
                        tempLabel = genLabel();
                    }
                    label.lFalse = tempLabel;
                } else {
                    label.lFalse = labels.lFalse;
                }
            }
            node.jjtGetChild(i).jjtAccept(this, label);
        }
        if(node.getOps().size() > 0) {
            node.jjtGetChild(node.jjtGetNumChildren() - 1).jjtAccept(this, labels);
            if(tempLabel != null) {
                m_writer.println(tempLabel);
            }
            return null;
        }
        return node.jjtGetChild(0).jjtAccept(this, data);
    }


    //cette fonction privé est utile parce que le code pour généré le goto pour les opérateurs de comparaison est le même
    //que celui pour le référencement de variable booléenne.
    //le code est très simple avant l'optimisation, mais deviens un peu plus long avec l'optimisation.
    private void genCodeRelTestJump(BoolLabel B, String test) {
        //version sans optimisation.
        if(B.lTrue != null && B.lFalse != null) {
            m_writer.println("if " + test + " goto " + B.lTrue);
            m_writer.println("goto " + B.lFalse);
        } else if(B.lTrue != null) {
            m_writer.println("if" + test + "goto" + B.lTrue);
        } else {
            m_writer.println("ifFalse " + test + " goto " + B.lFalse);
        }
    }


    //une partie de la fonction à été faite pour donner des pistes, mais comme tous le reste du fichier, tous est libre
    //à modification.
    /*
    À ajouté : la comparaison est plus complexe quand il s'agit d'une comparaison de booléen.
    Le truc est de :
    1. vérifier qu'il s'agit d'une comparaison de nombre ou de booléen.
        On peut Ce simplifier la vie et le déterminer simplement en regardant si les enfants retourne une valeur ou non, à condition
        de s'être assurer que les valeurs booléennes retourne toujours null.
     2. s'il s'agit d'une comparaison de nombre, on peut faire le code simple par "genCodeRelTestJump(B, test)"
     3. s'il s'agit d'une comparaison de booléen, il faut enregistrer la valeur gauche et droite de la comparaison dans une variable temporaire,
        en utilisant le même code que pour l'assignation, deux fois. (mettre ce code dans une fonction deviens alors pratique)
        avant de faire la comparaison "genCodeRelTestJump(B, test)" avec les deux variables temporaire.

        notez que cette méthodes peut sembler peu efficace pour certain cas, mais qu'avec des passes d'optimisations subséquente, (que l'on
        ne fera pas dans le cadre du TP), on pourrait s'assurer que le code produit est aussi efficace qu'il peut l'être.
     */
    @Override
    public Object visit(ASTCompExpr node, Object data) {
        if(node.jjtGetNumChildren() == 2) {
            BoolLabel labels = (BoolLabel) data;

            Object left = node.jjtGetChild(0).jjtAccept(this, null);
            Object right = node.jjtGetChild(1).jjtAccept(this, null);
            if(left != null && right != null) {
                genCodeRelTestJump(labels, String.format("%s %s %s", left, node.getValue(), right));
            } else {
                String tempLeft = genId();
                String tempRight = genId();
                generateAssignCode(node.jjtGetChild(0), tempLeft);
                generateAssignCode(node.jjtGetChild(1), tempRight);
                genCodeRelTestJump(labels, String.format("%s %s %s", tempLeft, node.getValue(), tempRight));
            }

            return null;
        }
        return node.jjtGetChild(0).jjtAccept(this, data);
    }


    /*
    Même si on peut y avoir un grand nombre d'opération, celle-ci s'annullent entre elle.
    il est donc intéressant de vérifier si le nombre d'opération est pair ou impaire.
    Si le nombre d'opération est pair, on peut simplement ignorer ce noeud.
     */
    @Override
    public Object visit(ASTNotExpr node, Object data) {
        int size = node.getOps().size();
        if(size % 2 == 1) {
            BoolLabel labels = (BoolLabel) data;
            BoolLabel temp = new BoolLabel();
            temp.lTrue = labels.lFalse;
            temp.lFalse = labels.lTrue;
        }
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTGenValue node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /*
    BoolValue ne peut pas simplement retourné sa valeur à son parent contrairement à GenValue et IntValue,
    Il doit plutôt généré des Goto direct, selon sa valeur.
     */
    @Override
    public Object visit(ASTBoolValue node, Object data) {
        if(data != null) {
            BoolLabel labels = (BoolLabel)data;
            String label = node.getValue() ? labels.lTrue : labels.lFalse;
            if(label != null) {
                m_writer.println(String.format("goto %s", label));
            }
        }
        return null;
    }


    /*
    si le type de la variable est booléenne, il faudra généré des goto ici.
    le truc est de faire un "if value == 1 goto Label".
    en effet, la structure "if valeurBool goto Label" n'existe pas dans la syntaxe du code à trois adresse.
     */
    @Override
    public Object visit(ASTIdentifier node, Object data) {
        if(SymbolTable.get(node.getValue()) == VarType.Bool) {
            if(data != null) {
                BoolLabel labels = (BoolLabel) data;
                String expr = node.getValue() + " == 1";
                genCodeRelTestJump(labels, expr);
            }
            return null;
        }
        return node.getValue();
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return Integer.toString(node.getValue());
    }

    //des outils pour vous simplifier la vie et vous enligner dans le travail
    public enum VarType {
        Bool,
        Number
    }

    //utile surtout pour envoyé de l'informations au enfant des expressions logiques.
    private class BoolLabel {
        public String lTrue = null;
        public String lFalse = null;
    }

    /* AJOUTER CECI A VOTRE FICHIER DU TP3 (partie 1) */
    @Override
    public Object visit(ASTSwitchStmt node, Object data) {
        String testLabel = genLabel();
        String nextLabel = genLabel();
        String t = node.jjtGetChild(0).jjtAccept(this, data).toString();
        LinkedHashMap<String, String> cases = new LinkedHashMap<String, String>();
        m_writer.println(String.format("goto %s", testLabel));

        for(int i = 1; i < node.jjtGetNumChildren(); ++i) {
            String label = genLabel();
            m_writer.println(label);
            String caseConst = (String)node.jjtGetChild(i).jjtAccept(this, null);
            cases.put(label, caseConst);
            m_writer.println(String.format("goto %s", nextLabel));
        }

        m_writer.println(testLabel);
        for(String i : cases.keySet()) {
            if(cases.get(i) != null) {
                m_writer.println(String.format("if %s == %s goto %s", t, cases.get(i), i));
            }
            else {
                m_writer.println(String.format("goto %s", i));
            }
        }
        m_writer.println(nextLabel);
        return null;
    }

    @Override
    public Object visit(ASTCaseStmt node, Object data) {
        Object caseConst = node.jjtGetChild(0).jjtAccept(this, null);
        node.jjtGetChild(1).jjtAccept(this, null);
        return caseConst;
    }

    @Override
    public Object visit(ASTDefaultStmt node, Object data) {

        node.childrenAccept(this, null);
        return null;
    }
}
