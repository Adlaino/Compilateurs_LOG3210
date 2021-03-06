/* Generated By:JJTree: Do not edit this line. ASTIntValue.java */
package analyzer.ast;

import java.util.Vector;

public class ASTOutNode extends SimpleNode {
  public ASTOutNode(int id) {
    super(id);
  }

  public ASTOutNode(Parser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(ParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  // PLB
  public int getStmtIndex() {
    return ((ASTIntValue)this.jjtGetChild(0)).getValue();
  }

  private Vector<String> m_lives = null;
  public void addLive(String var) { if(m_lives == null) m_lives = new Vector<String>(); m_lives.add(var); }
  public Vector<String> getLive() { return m_lives; }
}
