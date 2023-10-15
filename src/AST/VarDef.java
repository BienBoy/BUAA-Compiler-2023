package AST;

import CompilerError.*;
import SymbolTable.*;

public class VarDef extends BranchNode {
	@Override
	public void check(SymbolTable symbolTable) {
		int offset = 0;
		if (children.get(children.size() - 1) instanceof InitVal) {
			// 有初始化时，子结点比无初始化时多2个
			offset = 2;
		}
		LeafNode ident = (LeafNode) children.get(0);
		if (children.size() == 1 + offset) {
			// 普通变量
			Symbol symbol = new Variable(ident.getToken().getRawString());
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
			int shape = ((Calculable) children.get(2)).calculate();
			Symbol symbol = new Array1D(ident.getToken().getRawString(), shape);
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
			int shapeX = ((Calculable) children.get(2)).calculate();
			int shapeY = ((Calculable) children.get(5)).calculate();
			Symbol symbol = new Array2D(ident.getToken().getRawString(), shapeX, shapeY);
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
