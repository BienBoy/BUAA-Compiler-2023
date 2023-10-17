package AST;

import CompilerError.*;
import SymbolTable.SymbolTable;

public class StmtBreak extends Stmt {
	@Override
	public void check(SymbolTable symbolTable) {
		if (!inloop()) {
			LeafNode node = (LeafNode) children.get(0);
			// 记录错误
			ErrorRecord.add(new CompilerError(
					node.token.getLine(),
					ErrorType.INCORRECT_BREAK_CONTINUE,
					"在非循环块中使用了break语句"
			));
		}
	}

	private boolean inloop() {
		ASTNode node = this;
		while (node != null) {
			if (node instanceof StmtFor) {
				return true;
			}
			node = node.parent;
		}
		return false;
	}
}
