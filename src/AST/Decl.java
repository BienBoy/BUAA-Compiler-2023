package AST;

import java.io.BufferedWriter;
import java.io.IOException;

public class Decl extends BranchNode {
	@Override
	public void output(BufferedWriter writer) throws IOException {
		for (ASTNode child : children) {
			child.output(writer);
		}
	}
}
