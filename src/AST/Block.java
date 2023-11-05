package AST;

import SymbolTable.SymbolTable;

public class Block extends BranchNode {
	@Override
	public void check(SymbolTable symbolTable) {
		/* 对于Block，进入前需要建立新的符号表子表，
		 * 退出前需将当前所在表设为外层符号表子表 */
		symbolTable.create();
		super.check(symbolTable);
		symbolTable.changeToOuter();
	}

	/**
	 * 判断Block最后一条语句是否为return int语句
	 * @return Block最后一条语句是否为return语句
	 */
	public boolean lastReturnInt() {
		if (children.size() == 2) {
			// 空块
			return false;
		}
		// 最后一条语句
		ASTNode node = ((BlockItem) children.get(children.size() - 2)).children.get(0);
		if (!(node instanceof StmtReturn)) {
			// 最后一条不是return语句
			return false;
		}
		// 判断返回为空
		return !((StmtReturn) node).isVoid();
	}

	public boolean lastReturn() {
		if (children.size() == 2) {
			// 空块
			return false;
		}
		// 最后一条语句
		ASTNode node = ((BlockItem) children.get(children.size() - 2)).children.get(0);

		return node instanceof StmtReturn;
	}

	/**
	 * 返回Block右大括号'}'所在行号
	 * @return Block右大括号'}'所在行号
	 */
	public int getLastLine() {
		return ((LeafNode) children.get(children.size() - 1)).getToken().getLine();
	}
}
