package Parser;

import AST.*;
import AST.Number;
import CompilerError.*;
import Lexical.Token;
import Lexical.TokenType;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * 语法分析器
 */
public class Parser {
	// 词法分析得到的所有token
	private final ArrayList<Token> tokens;
	// 当前处理到的位置
	private int position = 0;

	// 记录当前循环嵌套层数，使用continue、break时，应不为0
	private int loop = 0;

	public Parser(ArrayList<Token> tokens) {
		this.tokens = tokens;
	}

	/**
	 * 对整个程序进行语法分析，返回生成的语法树的根结点
	 *
	 * @return 返回生成语法树根结点
	 */
	public ASTNode analyze() {
		return CompUnit();
	}

	/**
	 * 获取将要分析的TOKEN
	 *
	 * @return 将要分析的TOKEN
	 */
	private Token getToken() {
		// 为了回避异常，数组越界时返回一个NULL类型的Token
		if (position >= tokens.size()) {
			return new Token(TokenType.NULL, "", -1);
		}
		return tokens.get(position);
	}

	/**
	 * 预读TOKEN
	 *
	 * @param offset 偏移
	 * @return 预读的TOKEN
	 */
	private Token getToken(int offset) {
		// 为了回避异常，数组越界时返回一个NULL类型的Token
		if (position + offset >= tokens.size()) {
			return new Token(TokenType.NULL, "", -1);
		}
		return tokens.get(position + offset);
	}

	/**
	 * 处理下个TOKEN
	 */
	private void nextToken() {
		position++;
	}

	// 以下为所有非终结符的分析程序
	private CompUnit CompUnit() {
		CompUnit compUnit = new CompUnit();

		// 判断条件有点乱，统一封装为函数
		Supplier<Boolean> isDecl = () -> {
			// 超前扫描
			Token token0 = getToken(), token1 = getToken(1),
					token2 = getToken(2);
			if (token0.getType().equals(TokenType.CONSTTK))
				return true;
			return token0.getType().equals(TokenType.INTTK) &&
					!token1.getType().equals(TokenType.MAINTK) &&
					!token2.getType().equals(TokenType.LPARENT);
		};
		Supplier<Boolean> isFuncDef = () -> {
			// 超前扫描
			Token token0 = getToken(), token1 = getToken(1),
					token2 = getToken(2);
			if (token0.getType().equals(TokenType.VOIDTK))
				return true;
			return token0.getType().equals(TokenType.INTTK) &&
					!token1.getType().equals(TokenType.MAINTK) &&
					token2.getType().equals(TokenType.LPARENT);
		};
		Supplier<Boolean> isMainFuncDef = () -> {
			// 超前扫描
			Token token0 = getToken(), token1 = getToken(1);
			return token0.getType().equals(TokenType.INTTK) &&
					token1.getType().equals(TokenType.MAINTK);
		};

		while (isDecl.get()) {
			Decl decl = Decl();
			compUnit.append(decl);
		}

		while (isFuncDef.get()) {
			FuncDef funcDef = FuncDef();
			compUnit.append(funcDef);
		}

		if (!isMainFuncDef.get()) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "顺序应为{Decl} {FuncDef} MainFuncDef"
			));
			// 忽略错误，继续分析MainFuncDef
			while (!isMainFuncDef.get()) {
				nextToken();
			}
		}
		MainFuncDef mainFuncDef = MainFuncDef();
		compUnit.append(mainFuncDef);

		if (position < tokens.size()) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "多余的内容"
			));
			return compUnit;
		}

		return compUnit;
	}

	private Decl Decl() {
		Decl decl = new Decl();
		if (getToken().getType().equals(TokenType.CONSTTK)) {
			ConstDecl constDecl = ConstDecl();
			decl.append(constDecl);
			return decl;
		}
		VarDecl varDecl = VarDecl();
		decl.append(varDecl);
		return decl;
	}

	private ConstDecl ConstDecl() {
		ConstDecl constDecl = new ConstDecl();
		if (!getToken().getType().equals(TokenType.CONSTTK)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "缺少const"
			));
			return null;
		}
		constDecl.append(new LeafNode(getToken()));
		nextToken();

		BType bType = BType();
		constDecl.append(bType);

		ConstDef constDef = ConstDef();
		constDecl.append(constDef);

		while (getToken().getType().equals(TokenType.COMMA)) {
			constDecl.append(new LeafNode(getToken()));
			nextToken();

			constDef = ConstDef();
			constDecl.append(constDef);
		}

		if (!getToken().getType().equals(TokenType.SEMICN)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken(-1).getLine(), ErrorType.MISSING_SEMICOLON, "缺少分号"
			));
			// 忽略错误，补全分号，继续运行
			constDecl.append(new LeafNode(new Token(TokenType.SEMICN, ";", getToken().getLine())));
		} else {
			constDecl.append(new LeafNode(getToken()));
			nextToken();
		}

		return constDecl;
	}

	private BType BType() {
		BType bType = new BType();
		if (!getToken().getType().equals(TokenType.INTTK)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "应为int"
			));
			return null;
		}
		bType.append(new LeafNode(getToken()));
		nextToken();

		return bType;
	}

	private ConstDef ConstDef() {
		ConstDef constDef = new ConstDef();

		if (!getToken().getType().equals(TokenType.IDENFR)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "应为标识符"
			));
			return null;
		}
		constDef.append(new LeafNode(getToken()));
		nextToken();

		while (getToken().getType().equals(TokenType.LBRACK)) {
			constDef.append(new LeafNode(getToken()));
			nextToken();

			ConstExp constExp = ConstExp();
			constDef.append(constExp);

			if (!getToken().getType().equals(TokenType.RBRACK)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_BRACKET, "缺少右中括号']'"
				));
				// 忽略错误，补全中括号，继续运行
				constDef.append(new LeafNode(new Token(TokenType.RBRACK, "]", getToken().getLine())));
			} else {
				constDef.append(new LeafNode(getToken()));
				nextToken();
			}
		}

		if (!getToken().getType().equals(TokenType.ASSIGN)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken(-1).getLine(), ErrorType.OTHER, "定义常量时缺少赋值"
			));
			return constDef;
		}
		constDef.append(new LeafNode(getToken()));
		nextToken();

		ConstInitVal constInitVal = ConstInitVal();
		constDef.append(constInitVal);

		return constDef;
	}

	private ConstInitVal ConstInitVal() {
		ConstInitVal constInitVal = new ConstInitVal();

		if (getToken().getType().equals(TokenType.LBRACE)) {
			constInitVal.append(new LeafNode(getToken()));
			nextToken();

			if (getToken().getType().equals(TokenType.RBRACE)) {
				constInitVal.append(new LeafNode(getToken()));
				nextToken();
				return constInitVal;
			}

			ConstInitVal constInitVal1 = ConstInitVal();
			constInitVal.append(constInitVal1);

			while (getToken().getType().equals(TokenType.COMMA)) {
				constInitVal.append(new LeafNode(getToken()));
				nextToken();

				constInitVal1 = ConstInitVal();
				constInitVal.append(constInitVal1);
			}

			if (!getToken().getType().equals(TokenType.RBRACE)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.OTHER, "缺少右大括号'}'"
				));
				// 忽略错误，补全大括号，继续运行
				constInitVal.append(new LeafNode(new Token(TokenType.RBRACE, "}", getToken().getLine())));
			} else {
				constInitVal.append(new LeafNode(getToken()));
				nextToken();
			}

			return constInitVal;
		}

		ConstExp constExp = ConstExp();
		constInitVal.append(constExp);

		return constInitVal;
	}

	private VarDecl VarDecl() {
		VarDecl varDecl = new VarDecl();

		BType bType = BType();
		varDecl.append(bType);

		VarDef varDef = VarDef();
		varDecl.append(varDef);

		while (getToken().getType().equals(TokenType.COMMA)) {
			varDecl.append(new LeafNode(getToken()));
			nextToken();

			varDef = VarDef();
			varDecl.append(varDef);
		}

		if (!getToken().getType().equals(TokenType.SEMICN)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken(-1).getLine(), ErrorType.MISSING_SEMICOLON, "缺少分号"
			));
			// 忽略错误，补全分号，继续运行
			varDecl.append(new LeafNode(new Token(TokenType.SEMICN, ";", getToken().getLine())));
		} else {
			varDecl.append(new LeafNode(getToken()));
			nextToken();
		}

		return varDecl;
	}

	private VarDef VarDef() {
		VarDef varDef = new VarDef();

		if (!getToken().getType().equals(TokenType.IDENFR)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "应为标识符"
			));
			return null;
		}
		varDef.append(new LeafNode(getToken()));
		nextToken();

		while (getToken().getType().equals(TokenType.LBRACK)) {
			varDef.append(new LeafNode(getToken()));
			nextToken();

			ConstExp constExp = ConstExp();
			varDef.append(constExp);

			if (!getToken().getType().equals(TokenType.RBRACK)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_BRACKET, "缺少右中括号']'"
				));
				// 忽略错误，补全中括号，继续运行
				varDef.append(new LeafNode(new Token(TokenType.RBRACK, "}", getToken().getLine())));
			} else {
				varDef.append(new LeafNode(getToken()));
				nextToken();
			}
		}

		if (getToken().getType().equals(TokenType.ASSIGN)) {
			varDef.append(new LeafNode(getToken()));
			nextToken();

			InitVal initVal = InitVal();
			varDef.append(initVal);
		}

		return varDef;
	}

	private InitVal InitVal() {
		InitVal initVal = new InitVal();

		if (getToken().getType().equals(TokenType.LBRACE)) {
			initVal.append(new LeafNode(getToken()));
			nextToken();

			if (getToken().getType().equals(TokenType.RBRACE)) {
				initVal.append(new LeafNode(getToken()));
				nextToken();
				return initVal;
			}

			InitVal initVal1 = InitVal();
			initVal.append(initVal1);

			while (getToken().getType().equals(TokenType.COMMA)) {
				initVal.append(new LeafNode(getToken()));
				nextToken();

				initVal1 = InitVal();
				initVal.append(initVal1);
			}

			if (!getToken().getType().equals(TokenType.RBRACE)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.OTHER, "缺少右大括号'}'"
				));
				// 忽略错误，补全分号，继续运行
				initVal.append(new LeafNode(new Token(TokenType.RBRACE, "}", getToken().getLine())));
			} else {
				initVal.append(new LeafNode(getToken()));
				nextToken();
			}

			return initVal;
		}

		Exp exp = Exp();
		initVal.append(exp);

		return initVal;
	}

	private FuncDef FuncDef() {
		FuncDef funcDef = new FuncDef();

		FuncType funcType = FuncType();
		funcDef.append(funcType);

		if (!getToken().getType().equals(TokenType.IDENFR)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "应为标识符"
			));
			return null;
		}
		funcDef.append(new LeafNode(getToken()));
		nextToken();

		if (!getToken().getType().equals(TokenType.LPARENT)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "缺少左小括号'('"
			));
			// 忽略错误，补全小括号，继续运行
			funcDef.append(new LeafNode(new Token(TokenType.LPARENT, "(", getToken().getLine())));
		} else {
			funcDef.append(new LeafNode(getToken()));
			nextToken();
		}

		// 此处必须通过前缀判断，需要考虑右括号缺失的错误
		if (getToken().getType().equals(TokenType.INTTK)) {
			FuncFParams funcFParams = FuncFParams();
			funcDef.append(funcFParams);
		}
		if (!getToken().getType().equals(TokenType.RPARENT)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken(-1).getLine(), ErrorType.MISSING_PARENTHESIS, "缺少右小括号')'"
			));
			// 忽略错误，补全小括号，继续运行
			funcDef.append(new LeafNode(new Token(TokenType.RPARENT, ")", getToken().getLine())));
		} else {
			funcDef.append(new LeafNode(getToken()));
			nextToken();
		}

		Block block = Block();
		funcDef.append(block);

		return funcDef;
	}

	private MainFuncDef MainFuncDef() {
		MainFuncDef mainFuncDef = new MainFuncDef();

		if (!getToken().getType().equals(TokenType.INTTK)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken(-1).getLine(), ErrorType.OTHER, "应为int"
			));
			return null;
		}
		mainFuncDef.append(new LeafNode(getToken()));
		nextToken();

		if (!getToken().getType().equals(TokenType.MAINTK)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken(-1).getLine(), ErrorType.OTHER, "应为main"
			));
			return null;
		}
		mainFuncDef.append(new LeafNode(getToken()));
		nextToken();

		if (!getToken().getType().equals(TokenType.LPARENT)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "缺少左小括号'('"
			));
			// 忽略错误，补全小括号，继续运行
			mainFuncDef.append(new LeafNode(new Token(TokenType.LPARENT, "(", getToken().getLine())));
		} else {
			mainFuncDef.append(new LeafNode(getToken()));
			nextToken();
		}

		if (!getToken().getType().equals(TokenType.RPARENT)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken(-1).getLine(), ErrorType.MISSING_PARENTHESIS, "缺少右小括号')'"
			));
			// 忽略错误，补全小括号，继续运行
			mainFuncDef.append(new LeafNode(new Token(TokenType.RPARENT, ")", getToken().getLine())));
		} else {
			mainFuncDef.append(new LeafNode(getToken()));
			nextToken();
		}

		Block block = Block();
		mainFuncDef.append(block);

		return mainFuncDef;
	}

	private FuncType FuncType() {
		FuncType funcType = new FuncType();

		if (getToken().getType().equals(TokenType.VOIDTK)) {
			funcType.append(new LeafNode(getToken()));
			nextToken();
			return funcType;
		}

		if (getToken().getType().equals(TokenType.INTTK)) {
			funcType.append(new LeafNode(getToken()));
			nextToken();
			return funcType;
		}

		// 记录错误
		ErrorRecord.add(new CompilerError(
				getToken(-1).getLine(), ErrorType.OTHER, "不合法的函数返回值类型')'"
		));
		return funcType;
	}

	private FuncFParams FuncFParams() {
		FuncFParams funcFParams = new FuncFParams();

		FuncFParam funcFParam = FuncFParam();
		funcFParams.append(funcFParam);

		while (getToken().getType().equals(TokenType.COMMA)) {
			funcFParams.append(new LeafNode(getToken()));
			nextToken();

			funcFParam = FuncFParam();
			funcFParams.append(funcFParam);
		}

		return funcFParams;
	}

	private FuncFParam FuncFParam() {
		FuncFParam funcFParam = new FuncFParam();

		BType bType = BType();
		funcFParam.append(bType);

		if (!getToken().getType().equals(TokenType.IDENFR)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "应为标识符"
			));
			return null;
		}
		funcFParam.append(new LeafNode(getToken()));
		nextToken();

		if (getToken().getType().equals(TokenType.LBRACK)) {
			funcFParam.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.RBRACK)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_BRACKET, "缺少右中括号']'"
				));
				// 忽略错误，补全中括号，继续运行
				funcFParam.append(new LeafNode(new Token(TokenType.RBRACK, "]", getToken().getLine())));
			} else {
				funcFParam.append(new LeafNode(getToken()));
				nextToken();
			}

			while (getToken().getType().equals(TokenType.LBRACK)) {
				funcFParam.append(new LeafNode(getToken()));
				nextToken();

				ConstExp constExp = ConstExp();
				funcFParam.append(constExp);

				if (!getToken().getType().equals(TokenType.RBRACK)) {
					// 记录错误
					ErrorRecord.add(new CompilerError(
							getToken(-1).getLine(), ErrorType.MISSING_BRACKET, "缺少右中括号']'"
					));
					// 忽略错误，补全中括号，继续运行
					funcFParam.append(new LeafNode(new Token(TokenType.RBRACK, "]", getToken().getLine())));
				} else {
					funcFParam.append(new LeafNode(getToken()));
					nextToken();
				}
			}
		}

		return funcFParam;
	}

	private Block Block() {
		Block block = new Block();

		if (!getToken().getType().equals(TokenType.LBRACE)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "缺少左大括号'{'"
			));
			// 忽略错误，补全大括号，继续运行
			block.append(new LeafNode(new Token(TokenType.LBRACE, "{", getToken().getLine())));
		} else {
			block.append(new LeafNode(getToken()));
			nextToken();
		}

		// 考虑缺少右大括号的错误，只能通过前缀判断
		TokenType type = getToken().getType();
		while (type.equals(TokenType.CONSTTK) ||
				type.equals(TokenType.INTTK) ||
				type.equals(TokenType.IDENFR) ||
				type.equals(TokenType.SEMICN) ||
				type.equals(TokenType.PLUS) ||
				type.equals(TokenType.MINU) ||
				type.equals(TokenType.LPARENT) ||
				type.equals(TokenType.INTCON) ||
				type.equals(TokenType.LBRACE) ||
				type.equals(TokenType.IFTK) ||
				type.equals(TokenType.FORTK) ||
				type.equals(TokenType.BREAKTK) ||
				type.equals(TokenType.CONTINUETK) ||
				type.equals(TokenType.RETURNTK) ||
				type.equals(TokenType.PRINTFTK)) {
			BlockItem blockItem = BlockItem();
			block.append(blockItem);
			type = getToken().getType();
		}

		if (!getToken().getType().equals(TokenType.RBRACE)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken(-1).getLine(), ErrorType.OTHER, "缺少右大括号'}'"
			));
			// 忽略错误，补全大括号，继续运行
			block.append(new LeafNode(new Token(TokenType.RBRACE, "}", getToken().getLine())));
		} else {
			block.append(new LeafNode(getToken()));
			nextToken();
		}

		return block;
	}

	private BlockItem BlockItem() {
		BlockItem blockItem = new BlockItem();

		if (getToken().getType().equals(TokenType.CONSTTK) ||
				getToken().getType().equals(TokenType.INTTK)) {
			Decl decl = Decl();
			blockItem.append(decl);
			return blockItem;
		}

		Stmt stmt = Stmt();
		blockItem.append(stmt);
		return blockItem;
	}

	private Stmt Stmt() {
		Stmt stmt;

		if (getToken().getType().equals(TokenType.LBRACE)) {
			stmt = new StmtBlock();

			Block block = Block();
			stmt.append(block);

			return stmt;
		}

		if (getToken().getType().equals(TokenType.IFTK)) {
			stmt = new StmtIf();

			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.LPARENT)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken().getLine(), ErrorType.OTHER, "缺少左小括号'('"
				));
				// 忽略错误，补全小括号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.LPARENT, "(", getToken().getLine())));
			} else {
				stmt.append(new LeafNode(getToken()));
				nextToken();
			}

			Cond cond = Cond();
			stmt.append(cond);

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_PARENTHESIS, "缺少右小括号')'"
				));
				// 忽略错误，补全小括号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.RPARENT, ")", getToken().getLine())));
			} else {
				stmt.append(new LeafNode(getToken()));
				nextToken();
			}

			Stmt stmt1 = Stmt();
			stmt.append(stmt1);

			if (getToken().getType().equals(TokenType.ELSETK)) {
				stmt.append(new LeafNode(getToken()));
				nextToken();

				stmt1 = Stmt();
				stmt.append(stmt1);
			}

			return stmt;
		}

		if (getToken().getType().equals(TokenType.FORTK)) {
			stmt = new StmtFor();

			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.LPARENT)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken().getLine(), ErrorType.OTHER, "缺少左小括号'('"
				));
				// 忽略错误，补全小括号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.LPARENT, "(", getToken().getLine())));
			} else {
				stmt.append(new LeafNode(getToken()));
				nextToken();
			}

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				ForStmt forStmt = ForStmt();
				stmt.append(forStmt);
			}

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_SEMICOLON, "缺少分号"
				));
				// 忽略错误，补全分号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.SEMICN, ";", getToken().getLine())));
			} else {
				stmt.append(new LeafNode(getToken()));
				nextToken();
			}

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				Cond cond = Cond();
				stmt.append(cond);
			}

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_SEMICOLON, "缺少分号"
				));
				// 忽略错误，补全分号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.SEMICN, ";", getToken().getLine())));
			} else {
				stmt.append(new LeafNode(getToken()));
				nextToken();
			}

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				ForStmt forStmt = ForStmt();
				stmt.append(forStmt);
			}

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_PARENTHESIS, "缺少右小括号')'"
				));
				// 忽略错误，补全小括号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.RPARENT, ")", getToken().getLine())));
			} else {
				stmt.append(new LeafNode(getToken()));
				nextToken();
			}

			// 进入循环
			loop++;
			Stmt stmt1 = Stmt();
			stmt.append(stmt1);
			loop--;

			return stmt;
		}

		if (getToken().getType().equals(TokenType.BREAKTK) ||
				getToken().getType().equals(TokenType.CONTINUETK)) {
			if (getToken().getType().equals(TokenType.BREAKTK)) {
				stmt = new StmtBreak();
			} else {
				stmt = new StmtContinue();
			}

			if (loop == 0) {
				// loop为0表示不是在循环中使用的break或continue
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken().getLine(), ErrorType.INCORRECT_BREAK_CONTINUE,
						"在非循环块中使用了break或continue语句"
				));
			}

			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_SEMICOLON, "缺少分号"
				));
				return stmt;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			return stmt;
		}

		if (getToken().getType().equals(TokenType.RETURNTK)) {
			stmt = new StmtReturn();

			stmt.append(new LeafNode(getToken()));
			nextToken();

			TokenType type = getToken().getType();

			if (type.equals(TokenType.PLUS) || type.equals(TokenType.MINU) ||
					type.equals(TokenType.IDENFR) || type.equals(TokenType.LPARENT) ||
					type.equals(TokenType.INTCON)) {
				Exp exp = Exp();
				stmt.append(exp);
			}

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_SEMICOLON, "缺少分号"
				));
				// 忽略错误，补全分号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.SEMICN, ";", getToken().getLine())));
				return stmt;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			return stmt;
		}

		if (getToken().getType().equals(TokenType.PRINTFTK)) {
			stmt = new StmtPrintf();

			int printfLine = getToken().getLine(); // 记录printf所在行号，报错时可能使用

			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.LPARENT)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken().getLine(), ErrorType.OTHER, "缺少左小括号'('"
				));
				// 忽略错误，补全小括号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.LPARENT, "(", getToken().getLine())));
			} else {
				stmt.append(new LeafNode(getToken()));
				nextToken();
			}

			// 格式化字符串中的参数个数
			int params_num = 0;

			if (!getToken().getType().equals(TokenType.STRCON)) {
				ErrorRecord.add(new CompilerError(
						getToken().getLine(), ErrorType.OTHER, "应为格式化字符串"
				));
				// 忽略错误，补全分号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.STRCON, "", getToken().getLine())));
			} else {
				// 简单计算参数数量，假设%只与d同时出现
				String str = getToken().getRawString();
				for (int i = 0; i < str.length() - 1; ) {
					if (str.charAt(i) == '%' && str.charAt(i + 1) == 'd') {
						params_num++;
						i += 2;
						continue;
					}
					i++;
				}

				stmt.append(new LeafNode(getToken()));
				nextToken();
			}

			while (getToken().getType().equals(TokenType.COMMA)) {
				params_num--;

				stmt.append(new LeafNode(getToken()));
				nextToken();

				Exp exp = Exp();
				stmt.append(exp);
			}

			if (params_num != 0) {
				// 记录错误，printf中格式化字符串参数个数与表达式个数不匹配
				ErrorRecord.add(new CompilerError(
						printfLine, ErrorType.MISMATCHED_PRINTF_ARGS_NUM,
						"printf中格式化字符串参数个数与表达式个数不匹配"
				));
			}

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_PARENTHESIS, "缺少右小括号')'"
				));
				// 忽略错误，补全小括号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.RPARENT, ")", getToken().getLine())));
			} else {
				stmt.append(new LeafNode(getToken()));
				nextToken();
			}

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_SEMICOLON, "缺少分号"
				));
				// 忽略错误，补全分号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.SEMICN, ";", getToken().getLine())));
			} else {
				stmt.append(new LeafNode(getToken()));
				nextToken();
			}

			return stmt;
		}


		// 回溯
		int backup = position;
		ErrorRecord.close(); // 暂时关闭错误记录器
		LVal();
		boolean is_assign = getToken().getType().equals(TokenType.ASSIGN);
		position = backup;  // 回溯至原来位置
		ErrorRecord.open(); // 重新启用错误记录器

		if (is_assign) {
			// 假设为Stmt → LVal '=' Exp ';'
			stmt = new StmtAssign();

			LVal lVal = LVal();
			stmt.append(lVal);

			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (getToken().getType().equals(TokenType.GETINTTK)) {
				// 实际为Stmt → LVal '=' 'getint''('')'';'
				Stmt temp = new StmtGetInt();
				stmt.getChildren().forEach(temp::append);
				stmt = temp;
				
				stmt.append(new LeafNode(getToken()));
				nextToken();

				if (!getToken().getType().equals(TokenType.LPARENT)) {
					// 记录错误
					ErrorRecord.add(new CompilerError(
							getToken().getLine(), ErrorType.OTHER, "缺少左小括号'('"
					));
					// 忽略错误，补全小括号，继续运行
					stmt.append(new LeafNode(new Token(TokenType.LPARENT, "(", getToken().getLine())));
				} else {
					stmt.append(new LeafNode(getToken()));
					nextToken();
				}

				if (!getToken().getType().equals(TokenType.RPARENT)) {
					// 记录错误
					ErrorRecord.add(new CompilerError(
							getToken(-1).getLine(), ErrorType.MISSING_PARENTHESIS, "缺少右小括号')'"
					));
					// 忽略错误，补全小括号，继续运行
					stmt.append(new LeafNode(new Token(TokenType.RPARENT, ")", getToken().getLine())));
				} else {
					stmt.append(new LeafNode(getToken()));
					nextToken();
				}
			} else {
				Exp exp = Exp();
				stmt.append(exp);
			}

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_SEMICOLON, "缺少分号"
				));
				// 忽略错误，补全分号，继续运行
				stmt.append(new LeafNode(new Token(TokenType.SEMICN, ";", getToken().getLine())));
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			return stmt;
		}

		stmt = new StmtExp();
		TokenType type = getToken().getType();
		if (type.equals(TokenType.IDENFR) || type.equals(TokenType.PLUS) ||
				type.equals(TokenType.MINU) || type.equals(TokenType.LPARENT) ||
				type.equals(TokenType.INTCON)) {
			Exp exp = Exp();
			stmt.append(exp);
		}

		if (!getToken().getType().equals(TokenType.SEMICN)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken(-1).getLine(), ErrorType.MISSING_SEMICOLON, "缺少分号"
			));
			// 忽略错误，补全分号，继续运行
			stmt.append(new LeafNode(new Token(TokenType.SEMICN, ";", getToken().getLine())));
		}
		stmt.append(new LeafNode(getToken()));
		nextToken();

		return stmt;
	}

	private ForStmt ForStmt() {
		ForStmt forStmt = new ForStmt();

		LVal lVal = LVal();
		forStmt.append(lVal);

		if (!getToken().getType().equals(TokenType.ASSIGN)) {
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "应为'='"
			));
			// 忽略错误，补全小括号，继续运行
			forStmt.append(new LeafNode(new Token(TokenType.ASSIGN, "=", getToken().getLine())));
		} else {
			forStmt.append(new LeafNode(getToken()));
			nextToken();
		}

		Exp exp = Exp();
		forStmt.append(exp);

		return forStmt;
	}

	private Exp Exp() {
		Exp exp = new Exp();

		AddExp addExp = AddExp();
		exp.append(addExp);

		return exp;
	}

	private Cond Cond() {
		Cond cond = new Cond();

		LOrExp lOrExp = LOrExp();
		cond.append(lOrExp);

		return cond;
	}

	private LVal LVal() {
		LVal lVal = new LVal();

		if (!getToken().getType().equals(TokenType.IDENFR)) {
			// 记录错误
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "应为标识符"
			));
			return null;
		}
		lVal.append(new LeafNode(getToken()));
		nextToken();

		while (getToken().getType().equals(TokenType.LBRACK)) {
			lVal.append(new LeafNode(getToken()));
			nextToken();

			Exp exp = Exp();
			lVal.append(exp);

			if (!getToken().getType().equals(TokenType.RBRACK)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_BRACKET, "缺少右中括号']'"
				));
			} else {
				lVal.append(new LeafNode(getToken()));
				nextToken();
			}
		}

		return lVal;
	}

	private PrimaryExp PrimaryExp() {
		PrimaryExp primaryExp = new PrimaryExp();

		if (getToken().getType().equals(TokenType.LPARENT)) {
			primaryExp.append(new LeafNode(getToken()));
			nextToken();

			Exp exp = Exp();
			primaryExp.append(exp);

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_PARENTHESIS, "缺少右小括号')'"
				));
				// 忽略错误，补全小括号，继续运行
				primaryExp.append(new LeafNode(new Token(TokenType.RPARENT, ")", getToken().getLine())));
			} else {
				primaryExp.append(new LeafNode(getToken()));
				nextToken();
			}

			return primaryExp;
		}

		if (getToken().getType().equals(TokenType.IDENFR)) {
			LVal lVal = LVal();
			primaryExp.append(lVal);

			return primaryExp;
		}

		Number number = Number();
		primaryExp.append(number);

		return primaryExp;
	}

	private Number Number() {
		Number number = new Number();

		if (!getToken().getType().equals(TokenType.INTCON)) {
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "应为数字常量"
			));
			return null;
		}
		number.append(new LeafNode(getToken()));
		nextToken();

		return number;
	}

	private UnaryExp UnaryExp() {
		UnaryExp unaryExp = new UnaryExp();

		if (getToken().getType().equals(TokenType.PLUS) ||
				getToken().getType().equals(TokenType.MINU) ||
				getToken().getType().equals(TokenType.NOT)) {
			UnaryOp unaryOp = UnaryOp();
			unaryExp.append(unaryOp);

			UnaryExp unaryExp1 = UnaryExp();
			unaryExp.append(unaryExp1);

			return unaryExp;
		}

		// 超前扫描，消除回溯
		Token token0 = getToken(), token1 = getToken(1);

		if (token0.getType().equals(TokenType.IDENFR) &&
				token1.getType().equals(TokenType.LPARENT)) {
			unaryExp.append(new LeafNode(getToken()));
			nextToken();
			unaryExp.append(new LeafNode(getToken()));
			nextToken();

			// 考虑缺少右括号的错误，只能查看前缀确定
			TokenType type = getToken().getType();
			if (type.equals(TokenType.PLUS) ||
					type.equals(TokenType.MINU) ||
					type.equals(TokenType.LPARENT) ||
					type.equals(TokenType.IDENFR) ||
					type.equals(TokenType.INTCON)) {
				FuncRParams funcRParams = FuncRParams();
				unaryExp.append(funcRParams);
			}

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				// 记录错误
				ErrorRecord.add(new CompilerError(
						getToken(-1).getLine(), ErrorType.MISSING_PARENTHESIS, "缺少右小括号')'"
				));
				// 忽略错误，补全小括号，继续运行
				unaryExp.append(new LeafNode(new Token(TokenType.RPARENT, ")", getToken().getLine())));
			} else {
				unaryExp.append(new LeafNode(getToken()));
				nextToken();
			}

			return unaryExp;
		}

		PrimaryExp primaryExp = PrimaryExp();
		unaryExp.append(primaryExp);

		return unaryExp;
	}

	private UnaryOp UnaryOp() {
		UnaryOp unaryOp = new UnaryOp();

		if (!getToken().getType().equals(TokenType.PLUS) &&
				!getToken().getType().equals(TokenType.MINU) &&
				!getToken().getType().equals(TokenType.NOT)) {
			ErrorRecord.add(new CompilerError(
					getToken().getLine(), ErrorType.OTHER, "不合法的字符，应为'+'、'-'或'!'"
			));
			return null;
		} else {
			unaryOp.append(new LeafNode(getToken()));
			nextToken();
		}

		return unaryOp;
	}

	private FuncRParams FuncRParams() {
		FuncRParams funcRParams = new FuncRParams();

		Exp exp = Exp();
		funcRParams.append(exp);

		while (getToken().getType().equals(TokenType.COMMA)) {
			funcRParams.append(new LeafNode(getToken()));
			nextToken();

			exp = Exp();
			funcRParams.append(exp);
		}

		return funcRParams;
	}

	private MulExp MulExp() {
		MulExp mulExp = new MulExp();

		UnaryExp unaryExp = UnaryExp();
		mulExp.append(unaryExp);

		while (getToken().getType().equals(TokenType.MULT) ||
				getToken().getType().equals(TokenType.DIV) ||
				getToken().getType().equals(TokenType.MOD)) {
			mulExp.append(new LeafNode(getToken()));
			nextToken();

			unaryExp = UnaryExp();
			mulExp.append(unaryExp);
		}

		// 重新组织子树结构，以消除重写左递归文法带来的影响
		mulExp.reorganize();

		return mulExp;
	}

	private AddExp AddExp() {
		AddExp addExp = new AddExp();

		MulExp mulExp = MulExp();
		addExp.append(mulExp);

		while (getToken().getType().equals(TokenType.PLUS) ||
				getToken().getType().equals(TokenType.MINU)) {
			addExp.append(new LeafNode(getToken()));
			nextToken();

			mulExp = MulExp();
			addExp.append(mulExp);
		}

		// 重新组织子树结构，以消除重写左递归文法带来的影响
		addExp.reorganize();

		return addExp;
	}

	private RelExp RelExp() {
		RelExp relExp = new RelExp();

		AddExp addExp = AddExp();
		relExp.append(addExp);

		while (getToken().getType().equals(TokenType.LSS) ||
				getToken().getType().equals(TokenType.GRE) ||
				getToken().getType().equals(TokenType.LEQ) ||
				getToken().getType().equals(TokenType.GEQ)) {
			relExp.append(new LeafNode(getToken()));
			nextToken();

			addExp = AddExp();
			relExp.append(addExp);
		}

		// 重新组织子树结构，以消除重写左递归文法带来的影响
		relExp.reorganize();

		return relExp;
	}

	private EqExp EqExp() {
		EqExp eqExp = new EqExp();

		RelExp relExp = RelExp();
		eqExp.append(relExp);

		while (getToken().getType().equals(TokenType.EQL) ||
				getToken().getType().equals(TokenType.NEQ)) {
			eqExp.append(new LeafNode(getToken()));
			nextToken();

			relExp = RelExp();
			eqExp.append(relExp);
		}

		// 重新组织子树结构，以消除重写左递归文法带来的影响
		eqExp.reorganize();

		return eqExp;
	}

	private LAndExp LAndExp() {
		LAndExp lAndExp = new LAndExp();

		EqExp eqExp = EqExp();
		lAndExp.append(eqExp);

		while (getToken().getType().equals(TokenType.AND)) {
			lAndExp.append(new LeafNode(getToken()));
			nextToken();

			eqExp = EqExp();
			lAndExp.append(eqExp);
		}

		// 重新组织子树结构，以消除重写左递归文法带来的影响
		lAndExp.reorganize();

		return lAndExp;
	}

	private LOrExp LOrExp() {
		LOrExp lOrExp = new LOrExp();

		LAndExp lAndExp = LAndExp();
		lOrExp.append(lAndExp);

		while (getToken().getType().equals(TokenType.OR)) {
			lOrExp.append(new LeafNode(getToken()));
			nextToken();

			lAndExp = LAndExp();
			lOrExp.append(lAndExp);
		}

		// 重新组织子树结构，以消除重写左递归文法带来的影响
		lOrExp.reorganize();

		return lOrExp;
	}

	private ConstExp ConstExp() {
		ConstExp constExp = new ConstExp();

		AddExp addExp = AddExp();
		constExp.append(addExp);

		return constExp;
	}
}