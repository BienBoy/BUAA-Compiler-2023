package AST;

import SymbolTable.Array1D;
import SymbolTable.Array2D;
import SymbolTable.Symbol;
import SymbolTable.Variable;

public class FuncFParam extends BranchNode {
	public Symbol getParam() {
		LeafNode ident = (LeafNode) children.get(1);
		Symbol symbol;
		if (children.size() == 2) {
			// 普通变量
			symbol =  new Variable(ident.getToken().getRawString());
		} else if (children.size() == 4) {
			// 1维数组
			symbol =  new Array1D(ident.getToken().getRawString());
		} else {
			// 2维数组
			int shapeY = ((Calculable) children.get(5)).calculate();
			symbol = new Array2D(ident.getToken().getRawString(), shapeY);
		}
		// 保存叶结点对应的符号表项
		ident.symbol = symbol;
		return symbol;
	}

	public String getParamName() {
		LeafNode ident = (LeafNode) children.get(1);
		return ident.getToken().getRawString();
	}

	public int getIdentLine() {
		LeafNode ident = (LeafNode) children.get(1);
		return ident.getToken().getLine();
	}
}
