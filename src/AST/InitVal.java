package AST;

import CompilerError.*;

public class InitVal extends BranchNode {
	/**
	 * 初始化普通变量时，尝试求对应的值
	 * @return 普通变量的初始值，无法计算返回null
	 */
	public Integer calculateVarValue() {
		// 子结点应仅有一个Exp
		if (children.size() != 1) {
			ErrorRecord.add(new CompilerError(
					((LeafNode)children.get(0)).getToken().getLine(),
					ErrorType.OTHER,
					"不匹配的初始化"
			));
			// 忽略错误，继续运行
			return null;
		}
		// 计算Exp的值
		return ((Calculable)children.get(0)).calculate();
	}

	/**
	 * 初始化1维数组时，求对应的数组初始值
	 * @param shapeX 数组长度
	 * @return 1维数组
	 */
	public Integer[] calculateArray1D(int shapeX) {
		// 子结点应为'{' InitVal { ',' InitVal } '}'
		// 子结点中每个InitVal可计算出一个int值
		if (children.size() != 2 + shapeX * 2 - 1) {
			ErrorRecord.add(new CompilerError(
					((LeafNode)children.get(0)).getToken().getLine(),
					ErrorType.OTHER,
					"不匹配的初始化"
			));
			// 忽略错误，继续运行
			return new Integer[shapeX];
		}
		Integer[] array = new Integer[shapeX];
		for (int i = 0; i < shapeX; i++)
			array[i] = ((InitVal)children.get(1 + 2 * i)).calculateVarValue();
		return array;
	}

	/**
	 * 初始化2维数组时，求对应的数组初始值
	 * @param shapeX 第1维长度
	 * @param shapeY 第2维长度
	 * @return 2维常量数组
	 */
	public Integer[][] calculateArray2D(int shapeX, int shapeY) {
		// 子结点应为'{' InitVal { ',' InitVal } '}'
		// 子结点中每个InitVal可计算出一个1维int数组
		if (children.size() != 2 + shapeX * 2 - 1) {
			ErrorRecord.add(new CompilerError(
					((LeafNode)children.get(0)).getToken().getLine(),
					ErrorType.OTHER,
					"不匹配的初始化"
			));
			// 忽略错误，继续运行
			return new Integer[shapeX][shapeY];
		}
		Integer[][] array = new Integer[shapeX][shapeY];
		for (int i = 0; i < shapeX; i++)
			array[i] = ((InitVal)children.get(1 + 2 * i)).calculateArray1D(shapeY);
		return array;
	}
}
