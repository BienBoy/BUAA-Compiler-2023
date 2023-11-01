package AST;

import Lexical.TokenType;
import SymbolTable.*;
import CompilerError.*;

import java.util.ArrayList;

public class UnaryExp extends BranchNode implements Calculable, TypeAvailable {
	@Override
	public Integer calculate() {
		/*UnaryExp子结点情况有：PrimaryExp | Ident '(' [FuncRParams] ')'
		* | UnaryOp UnaryExp*/
		if (children.size() == 1) {
			// 子结点为PrimaryExp
			return ((Calculable)children.get(0)).calculate();
		}
		if (children.size() == 2) {
			// 子结点为UnaryOp UnaryExp
			UnaryOp unaryOp = (UnaryOp)children.get(0);
			TokenType type = ((LeafNode) unaryOp.children.get(0)).token.getType();
			Integer value = ((Calculable)children.get(1)).calculate();
			if (value == null) {
				return null;
			}
			if (type.equals(TokenType.PLUS)) {
				// 符号为+
				return value;
			}
			if (type.equals(TokenType.MINU)) {
				// 符号为-
				return -value;
			}
			// 符号为!，这种情况不应调用该函数
			return null;
		}
		// 子结点为Ident '(' [FuncRParams] ')'，即函数调用，不应调用函数
		return null;
	}

	@Override
	public Class<?> getType() {
		if (children.size() == 1) {
			return ((TypeAvailable) children.get(0)).getType();
		} else if (children.size() == 2) {
			// 对于UnaryOp UnaryExp，返回普通变量
			return Variable.class;
		}
		// 对于Ident '(' [FuncRParams] ')'，返回空值或普通变量
		LeafNode ident = (LeafNode) children.get(0);
		Function function = (Function) ident.symbol;
		if (function.hasReturn()) {
			return Variable.class;
		}
		return null;
	}

	@Override
	public void check(SymbolTable symbolTable) {
		/* 仅需考虑Ident '(' [FuncRParams] ')'。可能出现的错误有：
		 * 未定义的名字、函数参数个数不匹配或函数参数类型不匹配 */
		if (children.size() < 3) {
			super.check(symbolTable);
			return;
		}
		super.check(symbolTable);

		LeafNode ident = (LeafNode) children.get(0);
		Symbol symbol = symbolTable.search(ident.token.getRawString());

		if (symbol == null) {
			// 未定义的变量
			ErrorRecord.add(new CompilerError(
					ident.getToken().getLine(),
					ErrorType.UNDEFINED,
					"未定义的名字：" + ident.token.getRawString()
			));
		} else if (!(symbol instanceof Function)) {
			// 变量用作了函数
			ErrorRecord.add(new CompilerError(
					ident.getToken().getLine(),
					ErrorType.OTHER,
					"变量" + ident.token.getRawString() + "用作了函数"
			));
		} else {
			FuncRParams funcRParams = null;
			int rParamsNum = 0;
			ArrayList<Class<?>> rParamsType = null;
			if (children.size() == 4) {
				funcRParams = (FuncRParams) children.get(2);
				// 获取实参个数
				rParamsNum = funcRParams.getParamsNum();
				// 获取实参类型列表
				rParamsType = funcRParams.getParamsType();
			}


			// 形参
			Function function = (Function) symbol;

			if (!function.matchNum(rParamsNum)) {
				// 函数参数个数不匹配
				ErrorRecord.add(new CompilerError(
						ident.getToken().getLine(),
						ErrorType.MISMATCHED_FUNCTION_ARGS_NUM,
						"函数实参与形参个数不匹配"
				));
			} else if (!function.matchType(rParamsType)) {
				// 函数参数类型不匹配
				ErrorRecord.add(new CompilerError(
						ident.getToken().getLine(),
						ErrorType.MISMATCHED_FUNCTION_ARGS_TYPE,
						"函数实参与形参个数不匹配"
				));
			} else {
				// 保存叶结点对应的符号表项
				ident.symbol = symbol;
			}
		}
	}
}
