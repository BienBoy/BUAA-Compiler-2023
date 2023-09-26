package AST;

import java.io.BufferedWriter;
import java.io.IOException;

public abstract class ASTNode {
	public abstract void output(BufferedWriter writer) throws IOException;
}
