package AST;

public class Exp extends BranchNode implements Calculable, TypeAvailable {
	@Override
	public Integer calculate() {
		// Exp只有一个AddExp类型的子结点，直接调用子节点的calculate方法
		return ((Calculable) children.get(0)).calculate();
	}

	@Override
	public Class<?> getType() {
		return ((TypeAvailable) children.get(0)).getType();
	}
}
