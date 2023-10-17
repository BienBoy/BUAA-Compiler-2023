package AST;

import SymbolTable.SymbolTable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 语法树分支结点基类，所有分支结点均继承该类
 */
public abstract class BranchNode extends ASTNode {
	// 子结点
	protected final ArrayList<ASTNode> children;

	public BranchNode() {
		children = new ArrayList<>();
	}

	/**
	 * 添加子结点
	 * @param child 子结点
	 */
	public void append(ASTNode child){
		if (child == null) {
			return;
		}
		child.parent = this;
		children.add(child);
	}

	/**
	 * 获取子结点
	 * @return 一个包含所有子结点的ArrayList
	 */
	public ArrayList<ASTNode> getChildren() {
		return children;
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		for (ASTNode child : children) {
			child.output(writer);
		}
		writer.write("<" + this + ">");
		writer.newLine();
	}

	@Override
	public void check(SymbolTable symbolTable) {
		for (ASTNode child : children) {
			child.check(symbolTable);
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
