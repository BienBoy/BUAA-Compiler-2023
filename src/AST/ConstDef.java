package AST;

import CompilerError.*;
import SymbolTable.*;

public class ConstDef extends BranchNode {
	@Override
	public void check(SymbolTable symbolTable) {
		// 需要添加到符号表，可能有重定义错误
		LeafNode ident = (LeafNode) children.get(0);
		ConstInitVal constInitVal = (ConstInitVal) children.get(children.size() - 1);
		if (children.size() == 3) {
			// 普通常量
			int value = constInitVal.calculateConstValue();
			Symbol symbol = new Variable(ident.getToken().getRawString(), value);
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
			int shape = ((Calculable) children.get(2)).calculate();
			int[] value = constInitVal.calculateConstArray1D(shape);
			Symbol symbol = new Array1D(ident.getToken().getRawString(),
					shape, value);
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
			int shapeX = ((Calculable) children.get(2)).calculate();
			int shapeY = ((Calculable) children.get(5)).calculate();
			int[][] value = constInitVal.calculateConstArray2D(shapeX, shapeY);
			Symbol symbol = new Array2D(ident.getToken().getRawString(),
					shapeX, shapeY, value);
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
		super.check(symbolTable);
	}
}
