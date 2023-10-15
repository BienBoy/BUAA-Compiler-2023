package AST;

import CompilerError.*;
import SymbolTable.SymbolTable;

public class StmtAssign extends Stmt {
	@Override
	public void check(SymbolTable symbolTable) {
		super.check(symbolTable);
		// 需要检查是否可修改
		LVal lVal = (LVal) children.get(0);
		if (lVal.isConstant()) {
			// 错误：不能改变常量的值
			ErrorRecord.add(new CompilerError(
					lVal.getIdentLine(),
					ErrorType.MODIFIED_CONSTANT,
					lVal.getIdentName() + "不是可修改的左值"
			));
		}
	}
}
