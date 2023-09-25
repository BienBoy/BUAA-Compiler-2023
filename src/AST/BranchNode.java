package AST;

import java.util.ArrayList;

/**
 * 语法树结点基类
 */
public abstract class BranchNode extends ASTNode {
	// 子结点
	protected final ArrayList<ASTNode> children;

	public BranchNode() {
		children = new ArrayList<>();
	}

	// 添加子结点
	public void append(ASTNode child){
		children.add(child);
	}

	public ArrayList<ASTNode> getChildren() {
		return children;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
