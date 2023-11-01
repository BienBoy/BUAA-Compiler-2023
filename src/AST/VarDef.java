package AST;

import CompilerError.*;
import SymbolTable.*;

public class VarDef extends BranchNode {
	@Override
	public void check(SymbolTable symbolTable) {
		// 在有初始化语句的情况下，会计算初始化值，但仅有全局变量的初始化值是可靠的
		super.check(symbolTable);
		int offset = 0;
		if (children.get(children.size() - 1) instanceof InitVal) {
			// 有初始化时，子结点比无初始化时多2个
			offset = 2;
		}
		LeafNode ident = (LeafNode) children.get(0);
		if (children.size() == 1 + offset) {
			// 普通变量
			Integer value = null;
			if (offset == 2) {
				value = ((InitVal) children.get(children.size() - 1)).calculateVarValue();
			}
			Symbol symbol = new Variable(ident.getToken().getRawString(), value, false);
			if (!symbolTable.insert(symbol)) {
				// 重定义
				ErrorRecord.add(new CompilerError(
						ident.getToken().getLine(),
						ErrorType.REDEFINED,
						"名字重定义：" + ident.getToken().getRawString()
				));
			} else {
				// 插入成功后，保存叶结点对应的符号表项
				ident.symbol = symbol;
			}
		} else if (children.size() == 4 + offset) {
			// 1维数组
			Integer shape = ((Calculable) children.get(2)).calculate();
			if (shape == null) {
				ErrorRecord.add(new CompilerError(
						ident.token.getLine(),
						ErrorType.OTHER,
						"数组维度不可计算"
				));
				shape = 0;
			} else if (shape <= 0) {
				ErrorRecord.add(new CompilerError(
						ident.token.getLine(),
						ErrorType.OTHER,
						"数组维度只能为正整数"
				));
				shape = 0;
			}

			Integer[] value = null;
			if (offset == 2) {
				value = ((InitVal) children.get(children.size() - 1)).calculateArray1D(shape);
			}

			Symbol symbol = new Array1D(ident.getToken().getRawString(), shape, value, false);
			if (!symbolTable.insert(symbol)) {
				// 重定义
				ErrorRecord.add(new CompilerError(
						ident.getToken().getLine(),
						ErrorType.REDEFINED,
						"名字重定义：" + ident.getToken().getRawString()
				));
			} else {
				// 插入成功后，保存叶结点对应的符号表项
				ident.symbol = symbol;
			}
		} else {
			// 2维数组
			Integer shapeX = ((Calculable) children.get(2)).calculate();
			Integer shapeY = ((Calculable) children.get(5)).calculate();

			if (shapeX == null || shapeY == null) {
				ErrorRecord.add(new CompilerError(
						ident.token.getLine(),
						ErrorType.OTHER,
						"数组维度不可计算"
				));
				shapeX = shapeX == null ? 0 : shapeX;
				shapeY = shapeY == null ? 0 : shapeY;
			} else if (shapeX <= 0 || shapeY <= 0) {
				ErrorRecord.add(new CompilerError(
						ident.token.getLine(),
						ErrorType.OTHER,
						"数组维度只能为正整数"
				));
				shapeX = shapeY = 0;
			}

			Integer[][] value = null;
			if (offset == 2) {
				value = ((InitVal) children.get(children.size() - 1)).calculateArray2D(shapeX, shapeY);
			}

			Symbol symbol = new Array2D(ident.getToken().getRawString(), shapeX, shapeY, value, false);
			if (!symbolTable.insert(symbol)) {
				// 重定义
				ErrorRecord.add(new CompilerError(
						ident.getToken().getLine(),
						ErrorType.REDEFINED,
						"名字重定义：" + ident.getToken().getRawString()
				));
			} else {
				// 插入成功后，保存叶结点对应的符号表项
				ident.symbol = symbol;
			}
		}
	}
}
