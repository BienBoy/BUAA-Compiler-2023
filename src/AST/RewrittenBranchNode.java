package AST;

/**
 * 语法树中<b>改写了左递归文法</b>的分支结点基类。<br/>
 * 改写左递归文法会导致语法树结构改变，
 * 因此需要重新组织语法树结构。
 */
public abstract class RewrittenBranchNode extends BranchNode {
	/**
	 * 重新组织子树结构，消除改写文法的影响
	 */
	public abstract void reorganize();
}
