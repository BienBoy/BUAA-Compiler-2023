package AST;

import Lexical.TokenType;
import SymbolTable.Variable;

public class AddExp extends RewrittenBranchNode implements Calculable, TypeAvailable {
	@Override
	public void reorganize() {
		if (children.size() < 3)
			return;
		AddExp temp = new AddExp();
		temp.append(children.get(0));
		children.set(0, temp);
		while (children.size() > 3) {
			temp = new AddExp();
			for (int i = 0; i < 3; i++) {
				temp.append(children.get(0));
				children.remove(0);
			}
			children.add(0, temp);
		}
	}

	@Override
	public int calculate() {
		/* AddExp有1个或3个子结点，有1个子结点，则子结点为MulExp；
		*  有3个子结点，则分别为AddExp、LeafNode（+或-）和MulExp*/
		if (children.size() == 1) {
			return ((Calculable)children.get(0)).calculate();
		}
		int left = ((Calculable)children.get(0)).calculate();
		int right = ((Calculable)children.get(2)).calculate();
		if (((LeafNode)children.get(1)).getToken().getType().equals(TokenType.PLUS)) {
			// 操作符为+
			return left + right;
		}
		// 操作符为-
		return left - right;
	}

	@Override
	public Class<?> getType() {
		if (children.size() == 1) {
			return ((TypeAvailable) children.get(0)).getType();
		}
		return Variable.class;
	}
}
