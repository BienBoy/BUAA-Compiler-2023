package AST;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * 语法树结点基类，所有语法树结点（分支结点和叶子结点）均继承该类
 */
public abstract class ASTNode {
	/**
	 * 后序遍历，输出子树信息至文件
	 * @param writer 用于向目标文件写入内容的BufferedWriter
	 * @throws IOException 输出出错
	 */
	public abstract void output(BufferedWriter writer) throws IOException;
}
