package AST;

import SymbolTable.*;
import CompilerError.*;

import java.util.ArrayList;

public class FuncDef extends BranchNode {
	@Override
	public void check(SymbolTable symbolTable) {
		/* 需要将函数名添加到符号表，可能出现重定义；
		 * 此外，需要检查有返回值的函数最后一条语句是否为return。
		 * 而检查无返回值的函数是否返回了值放在在Stmt中判断 */
		boolean hasReturn = ((FuncType) children.get(0)).hasReturn();
		LeafNode ident = (LeafNode) children.get(1);
		ArrayList<Symbol> params = null;
		if (children.size() == 6) {
			// 有参数函数
			// 需要先调用FuncFParams的check
			children.get(3).check(symbolTable);
			params = ((FuncFParams) children.get(3)).getParams();
		}

		Symbol symbol = new Function(
				ident.getToken().getRawString(),
				hasReturn,
				params
		);

		// 填表
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

		// 保留形参，未来添加到内层符号表
		symbolTable.saveParams(params);

		Block block = (Block) children.get(children.size() - 1);
		// 调用Block的check
		block.check(symbolTable);

		// 需要检查有返回值的函数最后一条语句是否为return
		if (hasReturn && !block.lastReturn()) {
			// 最后一条语句不为return或返回为空
			ErrorRecord.add(new CompilerError(
					block.getLastLine(),
					ErrorType.MISSING_RETURN,
					"有返回值的函数" + ident.getToken().getRawString() + "最后一条语句不是return语句或返回空值"
			));
		}
	}
}
