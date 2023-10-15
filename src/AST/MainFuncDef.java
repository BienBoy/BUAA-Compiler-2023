package AST;

import CompilerError.*;
import SymbolTable.*;

public class MainFuncDef extends BranchNode {
	@Override
	public void check(SymbolTable symbolTable) {
		// 为了不出错，将main也加入符号表
		LeafNode ident = (LeafNode) children.get(1);
		Symbol symbol = new Function(
				ident.getToken().getRawString(),
				true,
				null
		);
		symbolTable.insert(symbol);

		super.check(symbolTable);

		Block block = (Block) children.get(4);
		// 需要检查有返回值的函数最后一条语句是否为return
		if (!block.lastReturn()) {
			// 最后一条语句不为return或返回为空
			ErrorRecord.add(new CompilerError(
					block.getLastLine(),
					ErrorType.MISSING_RETURN,
					"有返回值的函数" + ident.getToken().getRawString() + "最后一条语句不是return语句或返回空值"
			));
		}
	}
}
