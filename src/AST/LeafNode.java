package AST;

import Lexical.Token;

import java.io.BufferedWriter;
import java.io.IOException;

public class LeafNode extends ASTNode {
	private final Token token;

	public LeafNode(Token token) {
		this.token = token;
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		writer.write(this.toString());
		writer.newLine();
	}

	@Override
	public String toString() {
		return token.toString();
	}
}
