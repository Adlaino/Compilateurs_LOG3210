/* Generated By:JJTree: Do not edit this line. ASTIntValue.java */
package analyzer.ast;

import java.util.Vector;

public class ASTLiveNode extends SimpleNode {
  public ASTLiveNode(int id) {
    super(id);
  }

  public ASTLiveNode(Parser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(ParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  // PLB
  public int getStmtIndex() {
    return ((ASTIntValue)this.jjtGetChild(0).jjtGetChild(0)).getValue();
  }
}