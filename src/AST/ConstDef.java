package AST;

import CompilerError.*;
import SymbolTable.*;

public class ConstDef extends BranchNode {
	@Override
	public void check(SymbolTable symbolTable) {
		super.check(symbolTable);
		// 需要添加到符号表，可能有重定义错误
		LeafNode ident = (LeafNode) children.get(0);
		ConstInitVal constInitVal = (ConstInitVal) children.get(children.size() - 1);
		if (children.size() == 3) {
			// 普通常量
			Integer value = constInitVal.calculateConstValue();
			Symbol symbol = new Variable(ident.getToken().getRawString(), value, true);
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
		} else if (children.size() == 6) {
			// 一维数组
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
			Integer[] value = constInitVal.calculateConstArray1D(shape);
			Symbol symbol = new Array1D(ident.getToken().getRawString(),
					shape, value, true);
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
			// 二维数组
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
			Integer[][] value = constInitVal.calculateConstArray2D(shapeX, shapeY);
			Symbol symbol = new Array2D(ident.getToken().getRawString(),
					shapeX, shapeY, value, true);
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
