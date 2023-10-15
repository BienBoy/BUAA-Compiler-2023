package AST;

import Lexical.Token;
import SymbolTable.*;

import java.io.BufferedWriter;
import java.io.IOException;

public class LeafNode extends ASTNode {
	protected final Token token; // 存储的token
	protected Symbol symbol; // 标识符对应的符号表项

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

	public Token getToken() {
		return token;
	}

	public Symbol getSymbol() {
		return symbol;
	}

	@Override
	public void check(SymbolTable symbolTable) {
		return;
	}
}
