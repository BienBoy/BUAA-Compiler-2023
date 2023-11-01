package AST;

import SymbolTable.Variable;

public class PrimaryExp extends BranchNode implements Calculable, TypeAvailable {
	@Override
	public Integer calculate() {
		// 子结点有'(' Exp ')' | LVal | Number三种可能
		if (children.size() == 3) {
			// 子结点为'(' Exp ')'
			return ((Calculable)children.get(1)).calculate();
		}
		// 其他两种情况可以统一写成：
		return ((Calculable)children.get(0)).calculate();
	}

	@Override
	public Class<?> getType() {
		if (children.size() == 3) {
			// '(' Exp ')'，返回类型同Exp
			return ((TypeAvailable) children.get(1)).getType();
		}
		ASTNode node = children.get(0);
		if (node instanceof Number) {
			return Variable.class;
		}
		return ((TypeAvailable) node).getType();
	}
}
