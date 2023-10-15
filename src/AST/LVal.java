package AST;

import SymbolTable.*;
import CompilerError.*;

public class LVal extends BranchNode implements Calculable, TypeAvailable {
	/**
	 * 计算数值，应该在调用check之后再使用；
	 * 调用此函数时，参与运算的应均为常量，以保证可以在编译时计算出数值
	 *
	 * @return 数值
	 */
	@Override
	public int calculate() {
		// 子结点为Ident {'[' Exp ']'}，需要查表
		LeafNode ident = (LeafNode) children.get(0);
		Symbol symbol = ident.symbol;
		if (symbol instanceof Variable && children.size() == 1) {
			// 普通常量
			return ((Variable) symbol).getValue();
		}
		if (symbol instanceof Array1D && children.size() == 4) {
			// 1维数组
			int i = ((Exp) children.get(2)).calculate();
			return ((Array1D) symbol).getValue(i);
		}
		if (symbol instanceof Array2D && children.size() == 7) {
			// 2维数组
			int i = ((Exp) children.get(2)).calculate();
			int j = ((Exp) children.get(5)).calculate();
			return ((Array2D) symbol).getValue(i, j);
		}
		ErrorRecord.add(new CompilerError(
				ident.token.getLine(),
				ErrorType.OTHER,
				"不可计算出数值"
		));
		// 忽略错误，指定值为0，继续运行
		return 0;
	}

	@Override
	public Class<?> getType() {
		LeafNode ident = (LeafNode) children.get(0);
		Symbol symbol = ident.symbol;
		if (symbol == null) {
			// 出错的情况下，直接按普通变量处理
			return Variable.class;
		}
		if (symbol instanceof Variable) {
			return Variable.class;
		}
		if (symbol instanceof Array1D) {
			if (children.size() == 1) {
				return Array1D.class;
			}
			return Variable.class;
		}
		if (symbol instanceof Array2D) {
			if (children.size() == 1) {
				return Array2D.class;
			}
			if (children.size() == 4) {
				return Array1D.class;
			}
			return Variable.class;
		}
		return Variable.class;
	}

	@Override
	public void check(SymbolTable symbolTable) {
		super.check(symbolTable);
		// 需要查询符号表，可能有未定义错误
		LeafNode ident = (LeafNode) children.get(0);
		Symbol symbol = symbolTable.search(ident.token.getRawString());
		if (symbol == null) {
			// 未定义的变量
			ErrorRecord.add(new CompilerError(
					ident.getToken().getLine(),
					ErrorType.UNDEFINED,
					"未定义的名字：" + ident.token.getRawString()
			));
		} else if (symbol instanceof Function) {
			// 函数作为了左值
			ErrorRecord.add(new CompilerError(
					ident.getToken().getLine(),
					ErrorType.OTHER,
					"函数" + ident.token.getRawString() + "用作了左值"
			));
		} else if (symbol instanceof Variable && children.size() != 1) {
			// 普通变量用作了数组
			ErrorRecord.add(new CompilerError(
					ident.getToken().getLine(),
					ErrorType.OTHER,
					"普通变量" + ident.token.getRawString() + "用作了数组"
			));
		} else if (symbol instanceof Array1D && children.size() > 4) {
			// 1维数组用作了2维数组
			ErrorRecord.add(new CompilerError(
					ident.getToken().getLine(),
					ErrorType.OTHER,
					"1维数组" + ident.token.getRawString() + "用作了2维数组"
			));
		} else {
			// 插入成功后，保存叶结点对应的符号表项
			ident.symbol = symbol;
		}
	}

	/**
	 * 判断标识符对应是否为常量，应在check调用之后才能使用
	 *
	 * @return 是否为常量
	 */
	public boolean isConstant() {
		LeafNode ident = (LeafNode) children.get(0);
		Symbol symbol = ident.symbol;
		if (symbol == null || symbol instanceof Function) {
			return false;
		}
		return ((ConstSymbol) symbol).isConstant();
	}

	/**
	 * 获取标识符的行号
	 * @return 标识符的行号
	 */
	public int getIdentLine() {
		return ((LeafNode) children.get(0)).token.getLine();
	}

	/**
	 * 获取标识符名字
	 * @return 标识符名字
	 */
	public String getIdentName() {
		return ((LeafNode) children.get(0)).token.getRawString();
	}
}
