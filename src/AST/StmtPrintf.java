package AST;

import CompilerError.*;
import SymbolTable.SymbolTable;

public class StmtPrintf extends Stmt {
	@Override
	public void check(SymbolTable symbolTable) {
		super.check(symbolTable);

		LeafNode printf = (LeafNode) children.get(0);
		LeafNode strcon = (LeafNode) children.get(2);
		String str = strcon.token.getRawString();
		int params_num = 0;

		for (int i = 0; i < str.length() - 1; ) {
			if (str.charAt(i) == '%' && str.charAt(i + 1) == 'd') {
				params_num++;
				i += 2;
				continue;
			}
			i++;
		}

		if (children.size() != params_num * 2 + 5) {
			// 记录错误，printf中格式化字符串参数个数与表达式个数不匹配
			ErrorRecord.add(new CompilerError(
					printf.token.getLine(),
					ErrorType.MISMATCHED_PRINTF_ARGS_NUM,
					"printf中格式化字符串参数个数与表达式个数不匹配"
			));
		}
	}
}
