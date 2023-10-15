package AST;

public class Number extends BranchNode implements Calculable {
	@Override
	public int calculate() {
		// 子结点为LeafNode，里面存放了数值常量
		return ((LeafNode)children.get(0)).getToken().getIntValue();
	}
}
