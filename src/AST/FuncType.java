package AST;

import Lexical.TokenType;

public class FuncType extends BranchNode {
	public boolean hasReturn() {
		return ((LeafNode) children.get(0)).getToken().getType().equals(TokenType.INTTK);
	}
}
