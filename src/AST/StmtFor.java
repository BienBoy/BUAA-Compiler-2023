package AST;

public class StmtFor extends Stmt {
	public ForStmt getFirstForStmt() {
		if (children.get(2) instanceof ForStmt) {
			return (ForStmt) children.get(2);
		}
		return null;
	}

	public ForStmt getSecondForStmt() {
		if (children.get(children.size() - 3) instanceof ForStmt) {
			return (ForStmt) children.get(children.size() - 3);
		}
		return null;
	}

	public Cond getCond() {
		for (ASTNode child : children) {
			if (child instanceof Cond) {
				return (Cond) child;
			}
		}
		return null;
	}

	public Stmt getStmt() {
		return (Stmt) children.get(children.size() - 1);
	}
}
