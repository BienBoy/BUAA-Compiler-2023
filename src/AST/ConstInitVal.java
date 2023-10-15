package AST;

import CompilerError.*;

public class ConstInitVal extends BranchNode {
	/**
	 * 初始化普通常量时，求对应的值
	 * @return 普通常量的值
	 */
	public int calculateConstValue() {
		// 子结点应仅有一个ConstExp
		if (children.size() != 1) {
			ErrorRecord.add(new CompilerError(
					((LeafNode)children.get(0)).getToken().getLine(),
					ErrorType.OTHER,
					"不匹配的初始化"
			));
			// 忽略错误，继续运行
			return 0;
		}
		// 计算ConstExp的值
		return ((Calculable)children.get(0)).calculate();
	}

	/**
	 * 初始化1维常量数组时，求对应的数组
	 * @return 1维常量数组
	 */
	public int[] calculateConstArray1D(int shapeX) {
		// 子结点应为'{' ConstInitVal { ',' ConstInitVal } '}'
		// 子结点中每个ConstInitVal可计算出一个int值
		if (children.size() != 2 + shapeX * 2 - 1) {
			ErrorRecord.add(new CompilerError(
					((LeafNode)children.get(0)).getToken().getLine(),
					ErrorType.OTHER,
					"不匹配的初始化"
			));
			// 忽略错误，继续运行
			return new int[shapeX];
		}
		int[] array = new int[shapeX];
		for (int i = 0; i < shapeX; i++)
			array[i] = ((ConstInitVal)children.get(1 + 2 * i)).calculateConstValue();
		return array;
	}

	/**
	 * 初始化2维常量数组时，求对应的数组
	 * @param shapeX 第1维长度
	 * @param shapeY 第2维长度
	 * @return 2维常量数组
	 */
	public int[][] calculateConstArray2D(int shapeX, int shapeY) {
		// 子结点应为'{' ConstInitVal { ',' ConstInitVal } '}'
		// 子结点中每个ConstInitVal可计算出一个1维int数组
		if (children.size() != 2 + shapeX * 2 - 1) {
			ErrorRecord.add(new CompilerError(
					((LeafNode)children.get(0)).getToken().getLine(),
					ErrorType.OTHER,
					"不匹配的初始化"
			));
			// 忽略错误，继续运行
			return new int[shapeX][shapeY];
		}
		int[][] array = new int[shapeX][shapeY];
		for (int i = 0; i < shapeX; i++)
			array[i] = ((ConstInitVal)children.get(1 + 2 * i)).calculateConstArray1D(shapeY);
		return array;
	}
}
