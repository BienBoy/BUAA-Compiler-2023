package AST;

import Lexical.Token;

public class LeafNode extends ASTNode {
	private final Token token;

	public LeafNode(Token token) {
		this.token = token;
	}

	@Override
	public String toString() {
		return token.toString();
	}
}
