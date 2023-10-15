package AST;

import CompilerError.CompilerError;
import CompilerError.ErrorRecord;
import CompilerError.ErrorType;
import SymbolTable.Symbol;
import SymbolTable.SymbolTable;

import java.util.ArrayList;
import java.util.HashSet;

public class FuncFParams extends BranchNode {
	private ArrayList<Symbol> params;
	/**
	 * 获取所有参数，只能在调用check之后被使用
	 * @return 函数的所有形参
	 */
	public ArrayList<Symbol> getParams() {
		return params;
	}

	@Override
	public void check(SymbolTable symbolTable) {
		super.check(symbolTable);
		// 需要检查是否有重定义，此处的重定义的情况为多个多个形参同名
		HashSet<String> paramsName = new HashSet<>();
		params = new ArrayList<>();
		for (int i = 0; i < children.size(); i += 2) {
			Symbol param = ((FuncFParam) children.get(i)).getParam();
			if (paramsName.contains(param.getName())) {
				// 重定义
				ErrorRecord.add(new CompilerError(
						((FuncFParam) children.get(i)).getIdentLine(),
						ErrorType.REDEFINED,
						"名字重定义：" + param.getName()
				));
				continue;
			}
			paramsName.add(param.getName());
			params.add(param);
		}
	}
}
