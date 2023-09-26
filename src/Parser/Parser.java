package Parser;

import AST.*;
import AST.Number;
import Lexical.Token;
import Lexical.TokenType;

import java.util.ArrayList;

/**
 * 语法分析器
 */
public class Parser {
	// 词法分析得到的所有token
	private final ArrayList<Token> tokens;
	// 当前处理到的位置
	private int position = 0;

	public Parser(ArrayList<Token> tokens) {
		this.tokens = tokens;
	}

	/**
	 * 对整个程序进行语法分析，返回生成的语法树的根结点
	 * @return 返回生成语法树根结点
	 */
	public ASTNode analyze() {
		return CompUnit();
	}

	/**
	 * 获取将要分析的TOKEN
	 * @return 将要分析的TOKEN
	 */
	private Token getToken() {
		// TODO 抛出异常
		return tokens.get(position);
	}

	/**
	 * 预读TOKEN
	 * @param offset 偏移
	 * @return 预读的TOKEN
	 */
	private Token getToken(int offset) {
		// TODO 抛出异常
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
		// 存在一个小问题：未考虑Decl、FuncDef以及MainFuncDef的出现顺序
		CompUnit compUnit = new CompUnit();
		// 未获取到MainFuncDef时，循环分析
		while (true) {
			if (getToken().getType().equals(TokenType.CONSTTK)) {
				Decl decl = Decl();
				compUnit.append(decl);
				continue;
			}

			if (getToken().getType().equals(TokenType.VOIDTK)) {
				FuncDef funcDef = FuncDef();
				compUnit.append(funcDef);
				continue;
			}

			// 超前扫描
			Token token0 = getToken(), token1 = getToken(1),
					token2 = getToken(2);

			if (token0.getType().equals(TokenType.INTTK)) {
				if (token1.getType().equals(TokenType.MAINTK)) {
					MainFuncDef mainFuncDef = MainFuncDef();
					compUnit.append(mainFuncDef);
					break;
				}

				if (token2.getType().equals(TokenType.LPARENT)) {
					FuncDef funcDef = FuncDef();
					compUnit.append(funcDef);
					continue;
				}

				Decl decl = Decl();
				compUnit.append(decl);
				continue;
			}
			// TODO 抛出异常
			return null;
		}
		if (position < tokens.size()) {
			// TODO 抛出异常
			return null;
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
			// TODO 抛出异常
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
			// TODO 抛出异常
			return null;
		}
		constDecl.append(new LeafNode(getToken()));
		nextToken();

		return constDecl;
	}

	private BType BType() {
		BType bType = new BType();
		if (!getToken().getType().equals(TokenType.INTTK)) {
			// TODO 抛出异常
			return null;
		}
		bType.append(new LeafNode(getToken()));
		nextToken();
		return bType;
	}

	private ConstDef ConstDef() {
		ConstDef constDef = new ConstDef();

		if (!getToken().getType().equals(TokenType.IDENFR)) {
			// TODO 抛出异常
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
				// TODO 抛出异常
				return null;
			}
			constDef.append(new LeafNode(getToken()));
			nextToken();
		}

		if (!getToken().getType().equals(TokenType.ASSIGN)) {
			// TODO 抛出异常
			return null;
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
				// TODO 抛出异常
				return null;
			}
			constInitVal.append(new LeafNode(getToken()));
			nextToken();

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
			// TODO 抛出异常
			return null;
		}
		varDecl.append(new LeafNode(getToken()));
		nextToken();

		return varDecl;
	}

	private VarDef VarDef() {
		VarDef varDef = new VarDef();

		if (!getToken().getType().equals(TokenType.IDENFR)) {
			// TODO 抛出异常
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
				// TODO 抛出异常
				return null;
			}
			varDef.append(new LeafNode(getToken()));
			nextToken();
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
				// TODO 抛出异常
				return null;
			}
			initVal.append(new LeafNode(getToken()));
			nextToken();

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
			// TODO 抛出异常
			return null;
		}
		funcDef.append(new LeafNode(getToken()));
		nextToken();

		if (!getToken().getType().equals(TokenType.LPARENT)) {
			// TODO 抛出异常
			return null;
		}
		funcDef.append(new LeafNode(getToken()));
		nextToken();

		if (getToken().getType().equals(TokenType.RPARENT)) {
			funcDef.append(new LeafNode(getToken()));
			nextToken();
		} else {
			FuncFParams funcFParams = FuncFParams();
			funcDef.append(funcFParams);

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				// TODO 抛出异常
				return null;
			}
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
			// TODO 抛出异常
			return null;
		}
		mainFuncDef.append(new LeafNode(getToken()));
		nextToken();

		if (!getToken().getType().equals(TokenType.MAINTK)) {
			// TODO 抛出异常
			return null;
		}
		mainFuncDef.append(new LeafNode(getToken()));
		nextToken();

		if (!getToken().getType().equals(TokenType.LPARENT)) {
			// TODO 抛出异常
			return null;
		}
		mainFuncDef.append(new LeafNode(getToken()));
		nextToken();

		if (!getToken().getType().equals(TokenType.RPARENT)) {
			// TODO 抛出异常
			return null;
		}
		mainFuncDef.append(new LeafNode(getToken()));
		nextToken();

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

		// TODO 抛出异常
		return null;
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
			// TODO 抛出异常
			return null;
		}
		funcFParam.append(new LeafNode(getToken()));
		nextToken();

		if (getToken().getType().equals(TokenType.LBRACK)) {
			funcFParam.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.RBRACK)) {
				// TODO 抛出异常
				return null;
			}
			funcFParam.append(new LeafNode(getToken()));
			nextToken();

			while (getToken().getType().equals(TokenType.LBRACK)) {
				funcFParam.append(new LeafNode(getToken()));
				nextToken();

				ConstExp constExp = ConstExp();
				funcFParam.append(constExp);

				if (!getToken().getType().equals(TokenType.RBRACK)) {
					// TODO 抛出异常
					return null;
				}
				funcFParam.append(new LeafNode(getToken()));
				nextToken();
			}
		}

		return funcFParam;
	}

	private Block Block() {
		Block block = new Block();

		if (!getToken().getType().equals(TokenType.LBRACE)) {
			// TODO 抛出异常
			return null;
		}
		block.append(new LeafNode(getToken()));
		nextToken();

		while (!getToken().getType().equals(TokenType.RBRACE)) {
			BlockItem blockItem = BlockItem();
			block.append(blockItem);
		}

		block.append(new LeafNode(getToken()));
		nextToken();

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
		Stmt stmt = new Stmt();

		if (getToken().getType().equals(TokenType.LBRACE)) {
			Block block = Block();
			stmt.append(block);

			return stmt;
		}

		if (getToken().getType().equals(TokenType.IFTK)) {
			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.LPARENT)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			Cond cond = Cond();
			stmt.append(cond);

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

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
			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.LPARENT)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				ForStmt forStmt = ForStmt();
				stmt.append(forStmt);
			}

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				Cond cond = Cond();
				stmt.append(cond);
			}

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				ForStmt forStmt = ForStmt();
				stmt.append(forStmt);
			}

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			Stmt stmt1 = Stmt();
			stmt.append(stmt1);

			return stmt;
		}

		if (getToken().getType().equals(TokenType.BREAKTK) ||
				getToken().getType().equals(TokenType.CONTINUETK)) {
			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			return stmt;
		}

		if (getToken().getType().equals(TokenType.RETURNTK)) {
			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				Exp exp = Exp();
				stmt.append(exp);
			}

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			return stmt;
		}

		if (getToken().getType().equals(TokenType.PRINTFTK)) {
			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.LPARENT)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.STRCON)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			while (getToken().getType().equals(TokenType.COMMA)) {
				stmt.append(new LeafNode(getToken()));
				nextToken();

				Exp exp = Exp();
				stmt.append(exp);
			}

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			return stmt;
		}

		if (getToken().getType().equals(TokenType.SEMICN)) {
			stmt.append(new LeafNode(getToken()));
			nextToken();

			return stmt;
		}

		// 超前扫描避免回溯
		Token token0 = getToken(), token1 = getToken(1);

		if (token0.getType().equals(TokenType.IDENFR) &&
				(token1.getType().equals(TokenType.ASSIGN) ||
						token1.getType().equals(TokenType.LBRACK))) {
			LVal lVal = LVal();
			stmt.append(lVal);

			stmt.append(new LeafNode(getToken()));
			nextToken();

			if (getToken().getType().equals(TokenType.GETINTTK)) {
				stmt.append(new LeafNode(getToken()));
				nextToken();

				if (!getToken().getType().equals(TokenType.LPARENT)) {
					// TODO 抛出异常
					return null;
				}
				stmt.append(new LeafNode(getToken()));
				nextToken();

				if (!getToken().getType().equals(TokenType.RPARENT)) {
					// TODO 抛出异常
					return null;
				}
				stmt.append(new LeafNode(getToken()));
				nextToken();
			} else {
				Exp exp = Exp();
				stmt.append(exp);
			}

			if (!getToken().getType().equals(TokenType.SEMICN)) {
				// TODO 抛出异常
				return null;
			}
			stmt.append(new LeafNode(getToken()));
			nextToken();

			return stmt;
		}

		Exp exp = Exp();
		stmt.append(exp);

		if (!getToken().getType().equals(TokenType.SEMICN)) {
			// TODO 抛出异常
			return null;
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
			// TODO 抛出异常
			return null;
		}
		forStmt.append(new LeafNode(getToken()));
		nextToken();

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
			// TODO 抛出异常
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
				// TODO 抛出异常
				return null;
			}
			lVal.append(new LeafNode(getToken()));
			nextToken();
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
				// TODO 抛出异常
				return null;
			}
			primaryExp.append(new LeafNode(getToken()));
			nextToken();

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
			// TODO 抛出异常
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

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				FuncRParams funcRParams = FuncRParams();
				unaryExp.append(funcRParams);
			}

			if (!getToken().getType().equals(TokenType.RPARENT)) {
				// TODO 抛出异常
				return null;
			}
			unaryExp.append(new LeafNode(getToken()));
			nextToken();

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
			// TODO 抛出异常
			return null;
		}
		unaryOp.append(new LeafNode(getToken()));
		nextToken();

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