package AST;

public class ConstExp extends BranchNode implements Calculable {
	@Override
	public Integer calculate() {
		// ConstExp只有一个AddExp类型的子结点，直接调用子节点的calculate方法
		return ((Calculable)children.get(0)).calculate();
	}
}
