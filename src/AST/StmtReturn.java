package AST;

import CompilerError.*;
import SymbolTable.*;

public class StmtReturn extends Stmt {
	@Override
	public void check(SymbolTable symbolTable) {
		super.check(symbolTable);
		// 可能出现错误：无返回值的函数存在不匹配的return语句
		LeafNode node = (LeafNode) children.get(0);
		Function function = symbolTable.getCurrentFunction();
		if (!function.hasReturn() && children.size() == 3) {
			// 无返回值的函数存在不匹配的return语句
			ErrorRecord.add(new CompilerError(
					node.token.getLine(),
					ErrorType.MISMATCHED_RETURN,
					"无返回值的函数" + function.getName() + "存在不匹配的return语句"
			));
		}

		if (function.hasReturn() && children.size() == 2) {
			// 有返回值的函数返回了空值
			ErrorRecord.add(new CompilerError(
					node.token.getLine(),
					ErrorType.MISSING_RETURN,
					"有返回值的函数" + function.getName() + "返回了空值"
			));
		}
	}

	/**
	 * 判断return语句是否返回空
	 * @return 无返回值为true
	 */
	public boolean isVoid() {
		return children.size() == 2;
	}
}
