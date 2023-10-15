package AST;

import Lexical.TokenType;
import SymbolTable.Variable;

public class MulExp extends RewrittenBranchNode implements Calculable, TypeAvailable {
	@Override
	public void reorganize() {
		if (children.size() < 3)
			return;
		MulExp temp = new MulExp();
		temp.append(children.get(0));
		children.set(0, temp);
		while (children.size() > 3) {
			temp = new MulExp();
			for (int i = 0; i < 3; i++) {
				temp.append(children.get(0));
				children.remove(0);
			}
			children.add(0, temp);
		}
	}

	@Override
	public int calculate() {
		/* MulExp有1个或3个子结点，有1个子结点，则子结点为UnaryExp；
		 *  有3个子结点，则分别为MulExp、LeafNode（+或-）和UnaryExp*/
		if (children.size() == 1) {
			return ((Calculable)children.get(0)).calculate();
		}
		int left = ((Calculable)children.get(0)).calculate();
		int right = ((Calculable)children.get(2)).calculate();
		TokenType type = ((LeafNode)children.get(1)).getToken().getType();
		if (type.equals(TokenType.MULT)) {
			// 操作符为*
			return left * right;
		}
		if (type.equals(TokenType.DIV)) {
			// 操作符为/
			return left / right;
		}
		// 操作符为%
		return left % right;
	}

	@Override
	public Class<?> getType() {
		if (children.size() == 1) {
			return ((TypeAvailable) children.get(0)).getType();
		}
		return Variable.class;
	}
}
