package MidCode;

import AST.*;
import AST.Number;
import Lexical.TokenType;
import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import SymbolTable.Array1D;
import SymbolTable.Array2D;
import SymbolTable.Symbol;
import SymbolTable.Variable;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class IrBuilder {
	private int index;
	private final CompUnit root;
	private boolean addPrefix; // 生成LLVM IR时是否在名字中添加前缀
	private IrModule module;  // 构建的Module
	private Function currentFunction; // 当前所在的Function
	private BasicBlock currentBasicBlock; // 当前所在基本块
	private final LinkedHashMap<String, ConstString> constStrings = new LinkedHashMap<>();
	private final BasicBlock[] basicBlocks = new BasicBlock[2]; // 用于辅助生成跳转语句
	private final BasicBlock[] loopBasicBlocks = new BasicBlock[2]; // 用于辅助翻译continue、break

	public IrBuilder(CompUnit root) {
		this.root = root;
	}

	public IrBuilder(CompUnit root, boolean addPrefix) {
		this.root = root;
		this.addPrefix = addPrefix;
	}

	public IrModule generate() {
		generateModule();
		return module;
	}

	private void generateModule() {
		module = new IrModule();
		for (ASTNode node : root.getChildren()) {
			if (node instanceof Decl) {
				generateGlobalVariablesFromDecl((Decl) node);
				continue;
			}
			generateFunction((BranchNode) node);
		}
		// 加入字符串常量
		constStrings.values().forEach(module::addConstString);
	}

	//region 生成全局常量和全局变量
	private void generateGlobalVariablesFromDecl(Decl decl) {
		ASTNode node = decl.getChildren().get(0);
		if (node instanceof ConstDecl) {
			generateGlobalVariableFromConstDecl((ConstDecl) node);
			return;
		}
		generateGlobalVariableFromVarDecl((VarDecl) node);
	}

	//region 生成全局常量
	private void generateGlobalVariableFromConstDecl(ConstDecl constDecl) {
		ArrayList<GlobalVariable> globalVariables = new ArrayList<>();
		for (ASTNode node : constDecl.getChildren()) {
			if (node instanceof ConstDef) {
				generateGlobalVariableFromConstDef((ConstDef) node);
			}
		}
	}

	private void generateGlobalVariableFromConstDef(ConstDef constDef) {
		LeafNode ident = (LeafNode) constDef.getChildren().get(0);
		Symbol symbol = ident.getSymbol();
		module.addGlobalVariable(generateGlobalVariable(symbol));
	}
	//endregion

	//region 生成全局变量
	private void generateGlobalVariableFromVarDecl(VarDecl varDecl) {
		for (ASTNode node : varDecl.getChildren()) {
			if (node instanceof VarDef) {
				generateGlobalVariableFromVarDef((VarDef) node);
			}
		}
	}

	private void generateGlobalVariableFromVarDef(VarDef varDef) {
		LeafNode ident = (LeafNode) varDef.getChildren().get(0);
		Symbol symbol = ident.getSymbol();
		module.addGlobalVariable(generateGlobalVariable(symbol));
	}
	//endregion

	//endregion


	//region 生成函数
	private void generateFunction(BranchNode functionDef) {
		index = 0; // index清空

		LeafNode indent = (LeafNode) functionDef.getChildren().get(1);
		Symbol symbol = indent.getSymbol();
		currentFunction = generateFunction(symbol);
		module.addFunction(currentFunction);

		ArrayList<Symbol> params = ((SymbolTable.Function) symbol).getParams();
		// 形参编号
		if (params != null) {
			for (Symbol param : params) {
				currentFunction.addParam(generateFunctionParam(param));
			}
		}

		// 生成基本块
		Block block = (Block) functionDef.getChildren().get(functionDef.getChildren().size() - 1);
		generateBasicBlocksFromBlock(block, true);

		// 返回值为空的函数最后一句不是return;时，需要添加ret void
		if (!block.lastReturn()) {
			currentBasicBlock.add(generateRet());
		}
	}
	//endregion

	// 生成基本块
	private void generateBasicBlocksFromBlock(Block block, boolean addParams) {
		// 源代码中的Block不一定对应一个基本块
		currentBasicBlock = generateBasicBlock();
		currentFunction.addBasicBlock(currentBasicBlock);

		if (addParams) {
			// 为形参生成alloc语句
			for (Value param : currentFunction.getParams()) {
				currentBasicBlock.add(generateAlloca(param.getSymbol()));
			}

			// 为所有局部变量生成alloc语句
			Symbol symbol = currentFunction.getSymbol();
			ArrayList<Symbol> symbols = symbol.getSubSymbolTable().getAllSubSymbol((SymbolTable.Function) symbol);
			for (int i = currentFunction.getParams().size(); i < symbols.size(); i++) {
				currentBasicBlock.add(generateAlloca(symbols.get(i)));
			}

			// 为形参生成store语句
			for (Value param : currentFunction.getParams()) {
				currentBasicBlock.add(generateStore(param, param.getSymbol().getIRValue()));
			}
		}

		// 生成指令
		generateInstructionsFromBlock(block);
	}

	private void generateInstructionsFromBlock(Block block) {
		for (ASTNode node : block.getChildren()) {
			if (node instanceof BlockItem) {
				generateInstructionsFromBlockItem((BlockItem) node);
				ASTNode temp = ((BlockItem) node).getChildren().get(0);
			}
		}
	}

	private void generateInstructionsFromBlockItem(BlockItem blockItem) {
		ASTNode node = blockItem.getChildren().get(0);
		if (node instanceof Decl) {
			// 局部变量生成指令，仅在有初始化的情况下生成store指令
			generateInstructionsFromDecl((Decl) node);
		} else {
			generateInstructionsFromStmt((Stmt) node);
		}
	}

	private void generateInstructionsFromStmt(Stmt stmt) {
		if (stmt instanceof StmtAssign) {
			generateInstructionsFromStmtAssign((StmtAssign) stmt);
		} else if (stmt instanceof StmtBlock) {
			generateInstructionsFromStmtBlock((StmtBlock) stmt);
		} else if (stmt instanceof StmtBreak) {
			generateInstructionsFromStmtBreak((StmtBreak) stmt);
		} else if (stmt instanceof StmtContinue) {
			generateInstructionsFromStmtContinue((StmtContinue) stmt);
		} else if (stmt instanceof StmtExp) {
			generateInstructionsFromStmtExp((StmtExp) stmt);
		} else if (stmt instanceof StmtFor) {
			generateInstructionsFromStmtFor((StmtFor) stmt);
		} else if (stmt instanceof StmtGetInt) {
			generateInstructionsFromStmtGetInt((StmtGetInt) stmt);
		} else if (stmt instanceof StmtIf) {
			generateInstructionsFromStmtIf((StmtIf) stmt);
		} else if (stmt instanceof StmtPrintf) {
			generateInstructionsFromStmtPrintf((StmtPrintf) stmt);
		} else if (stmt instanceof StmtReturn) {
			generateInstructionsFromStmtReturn((StmtReturn) stmt);
		}
	}

	private void generateInstructionsFromStmtAssign(StmtAssign stmtAssign) {
		LVal lVal = (LVal) stmtAssign.getChildren().get(0);
		Exp exp = (Exp) stmtAssign.getChildren().get(2);
		Value address = generateWriteInstructionsFromLVal(lVal);
		Value value = generateInstructionsFromExp(exp);
		currentBasicBlock.add(generateStore(value, address));
	}

	private void generateInstructionsFromStmtBlock(StmtBlock stmtBlock) {
		Block block = (Block) stmtBlock.getChildren().get(0);
		generateInstructionsFromBlock(block);
	}

	private void generateInstructionsFromStmtBreak(StmtBreak stmtBreak) {
		currentBasicBlock.add(generateBr(loopBasicBlocks[1]));
		currentBasicBlock = generateBasicBlock();
		currentFunction.addBasicBlock(currentBasicBlock);
	}

	private void generateInstructionsFromStmtContinue(StmtContinue stmtContinue) {
		currentBasicBlock.add(generateBr(loopBasicBlocks[0]));
		currentBasicBlock = generateBasicBlock();
		currentFunction.addBasicBlock(currentBasicBlock);
	}

	private void generateInstructionsFromStmtExp(StmtExp stmtExp) {
		if (stmtExp.getChildren().size() == 2) {
			Exp exp = (Exp) stmtExp.getChildren().get(0);
			generateInstructionsFromExp(exp);
		}
	}

	private void generateInstructionsFromStmtFor(StmtFor stmtFor) {
		ForStmt forStmt1 = stmtFor.getFirstForStmt();
		ForStmt forStmt2 = stmtFor.getSecondForStmt();
		Cond cond = stmtFor.getCond();

		if (forStmt1 != null) {
			generateInstructionsFromForStmt(forStmt1);
		}

		BasicBlock temp = generateBasicBlock();
		currentBasicBlock.add(generateBr(temp));

		// 进入判断条件所在的基本块
		currentBasicBlock = temp;
		currentFunction.addBasicBlock(currentBasicBlock);

		basicBlocks[0] = pregenerateBasicBlock();
		basicBlocks[1] = pregenerateBasicBlock();

		if (cond != null) {
			generateInstructionsFromCond(cond);
		} else {
			// 没有判断语句。直接跳转即可
			currentBasicBlock.add(generateBr(basicBlocks[0]));
		}

		// 进入循环体块
		currentBasicBlock = basicBlocks[0];
		currentFunction.addBasicBlock(currentBasicBlock);
		nameBasicBlock(currentBasicBlock);

		// 备份循环体外的BasicBlock
		BasicBlock temp2 = loopBasicBlocks[0];
		BasicBlock temp3 = loopBasicBlocks[1];

		loopBasicBlocks[0] = pregenerateBasicBlock();
		loopBasicBlocks[1] = basicBlocks[1];

		Stmt stmt = stmtFor.getStmt();
		generateInstructionsFromStmt(stmt);
		if (currentBasicBlock.getValues().isEmpty() ||
				!(currentBasicBlock.getValues().get(currentBasicBlock.getValues().size() - 1) instanceof Ret) ||
				!(currentBasicBlock.getValues().get(currentBasicBlock.getValues().size() - 1) instanceof Br)
		) {
			currentBasicBlock.add(generateBr(loopBasicBlocks[0]));
		}

		currentBasicBlock = loopBasicBlocks[0];
		currentFunction.addBasicBlock(currentBasicBlock);
		nameBasicBlock(currentBasicBlock);
		if (forStmt2 != null) {
			generateInstructionsFromForStmt(forStmt2);
		}
		currentBasicBlock.add(generateBr(temp));

		// 离开循环
		currentBasicBlock = loopBasicBlocks[1];
		currentFunction.addBasicBlock(currentBasicBlock);
		nameBasicBlock(currentBasicBlock);
		loopBasicBlocks[0] = temp2;
		loopBasicBlocks[1] = temp3;
	}

	private void generateInstructionsFromForStmt(ForStmt forStmt) {
		LVal lVal = (LVal) forStmt.getChildren().get(0);
		Exp exp = (Exp) forStmt.getChildren().get(2);
		Value address = generateWriteInstructionsFromLVal(lVal);
		Value value = generateInstructionsFromExp(exp);
		currentBasicBlock.add(generateStore(value, address));
	}

	private void generateInstructionsFromStmtGetInt(StmtGetInt stmtGetInt) {
		LVal lVal = (LVal) stmtGetInt.getChildren().get(0);
		Value address = generateWriteInstructionsFromLVal(lVal);
		GetInt getInt = generateGetInt();
		currentBasicBlock.add(getInt);
		currentBasicBlock.add(generateStore(getInt, address));
	}

	private void generateInstructionsFromStmtIf(StmtIf stmtIf) {
		// if语句有跳转，会增加基本块的数量
		basicBlocks[0] = pregenerateBasicBlock();
		basicBlocks[1] = pregenerateBasicBlock();

		Cond cond = (Cond) stmtIf.getChildren().get(2);
		generateInstructionsFromCond(cond);

		if (stmtIf.getChildren().size() == 5) {
			// 进入if块
			currentBasicBlock = basicBlocks[0];
			currentFunction.addBasicBlock(currentBasicBlock);
			nameBasicBlock(currentBasicBlock);
			// 备份if之后的BasicBlock
			BasicBlock temp = basicBlocks[1];
			Stmt ifStmt = (Stmt) stmtIf.getChildren().get(4);
			generateInstructionsFromStmt(ifStmt);
			if (currentBasicBlock.getValues().isEmpty() ||
					!(currentBasicBlock.getValues().get(currentBasicBlock.getValues().size() - 1) instanceof Ret) &&
					!(currentBasicBlock.getValues().get(currentBasicBlock.getValues().size() - 1) instanceof Br)) {
				currentBasicBlock.add(generateBr(temp));
			}

			currentBasicBlock = temp;
			currentFunction.addBasicBlock(currentBasicBlock);
			nameBasicBlock(currentBasicBlock);
		} else {
			// 进入if块
			currentBasicBlock = basicBlocks[0];
			currentFunction.addBasicBlock(currentBasicBlock);
			nameBasicBlock(currentBasicBlock);
			// 备份else块
			BasicBlock temp = basicBlocks[1];
			// 预构建else之后的BasicBlock
			BasicBlock temp2 = pregenerateBasicBlock();

			Stmt ifStmt = (Stmt) stmtIf.getChildren().get(4);
			generateInstructionsFromStmt(ifStmt);
			if (currentBasicBlock.getValues().isEmpty() || !(currentBasicBlock.getValues().get(currentBasicBlock.getValues().size() - 1) instanceof Ret)) {
				currentBasicBlock.add(generateBr(temp2));
			}

			//进入else块
			currentBasicBlock = temp;
			currentFunction.addBasicBlock(currentBasicBlock);
			nameBasicBlock(currentBasicBlock);
			Stmt elseStmt = (Stmt) stmtIf.getChildren().get(6);
			generateInstructionsFromStmt(elseStmt);
			if (currentBasicBlock.getValues().isEmpty() || !(currentBasicBlock.getValues().get(currentBasicBlock.getValues().size() - 1) instanceof Ret)) {
				currentBasicBlock.add(generateBr(temp2));
			}

			currentBasicBlock = temp2;
			currentFunction.addBasicBlock(currentBasicBlock);
			nameBasicBlock(currentBasicBlock);
		}
	}

	private void generateInstructionsFromStmtPrintf(StmtPrintf stmtPrintf) {
		String str = ((LeafNode) stmtPrintf.getChildren().get(2)).getToken().getRawString();
		str = str.substring(1, str.length() - 1).replace("\\n", "\n");
		String[] substrs = str.split("(?=%d)|(?<=%d)");
		for (int i = 0, j = 0; i < substrs.length; i++) {
			if (substrs[i].equals("%d")) {
				Exp exp = (Exp) stmtPrintf.getChildren().get(4 + 2 * j++);
				Value value = generateInstructionsFromExp(exp);
				currentBasicBlock.add(generatePutint(value));
				continue;
			}
			ConstString constString = constStrings.get(substrs[i]);
			if (constString == null) {
				constString = generateConstString(substrs[i]);
				constStrings.put(substrs[i], constString);
			}
			currentBasicBlock.add(generatePutstr(constString));
		}
	}

	private void generateInstructionsFromStmtReturn(StmtReturn stmtReturn) {
		ASTNode node = (stmtReturn).getChildren().get(1);
		if (node instanceof Exp) {
			Value value = generateInstructionsFromExp((Exp) node);
			currentBasicBlock.add(generateRet(value));
		} else {
			currentBasicBlock.add(generateRet());
		}
		currentBasicBlock = generateBasicBlock();
		currentFunction.addBasicBlock(currentBasicBlock);
	}

	private void generateInstructionsFromDecl(Decl decl) {
		ASTNode node = decl.getChildren().get(0);
		if (node instanceof ConstDecl) {
			generateInstructionsFromConstDecl((ConstDecl) node);
		} else {
			generateInstructionsFromVarDecl((VarDecl) node);
		}
	}

	private void generateInstructionsFromConstDecl(ConstDecl constDecl) {
		for (ASTNode node : constDecl.getChildren()) {
			if (node instanceof ConstDef) {
				generateInstructionsFromConstDef((ConstDef) node);
			}
		}
	}

	private void generateInstructionsFromVarDecl(VarDecl varDecl) {
		for (ASTNode node : varDecl.getChildren()) {
			if (node instanceof VarDef) {
				generateInstructionsFromVarDef((VarDef) node);
			}
		}
	}

	private void generateInstructionsFromConstDef(ConstDef constDef) {
		LeafNode ident = (LeafNode) constDef.getChildren().get(0);
		Symbol symbol = ident.getSymbol();

		if (symbol instanceof Variable) {
			// 展开计算ConstInitVal
			ConstInitVal constInitVal = (ConstInitVal) constDef.getChildren().get(2);
			ConstExp constExp = (ConstExp) constInitVal.getChildren().get(0);
			Value value = generateInstructionsFromConstExp(constExp);
			currentBasicBlock.add(generateStore(value, symbol.getIRValue()));
		} else if (symbol instanceof Array1D) {
			ConstInitVal constInitVal = (ConstInitVal) constDef.getChildren().get(5);
			Array1D array = (Array1D) symbol;
			for (int i = 0; i < array.getShape(); i++) {
				Value address = generateGetelementptr(
						symbol.getIRValue(),
						new ConstInt(0),
						new ConstInt(i)
				);
				currentBasicBlock.add(address);
				ConstInitVal constInitVal2 = (ConstInitVal) constInitVal.getChildren().get(1 + 2 * i);
				ConstExp constExp = (ConstExp) constInitVal2.getChildren().get(0);
				Value value = generateInstructionsFromConstExp(constExp);
				currentBasicBlock.add(generateStore(value, address));
			}
		} else {
			ConstInitVal constInitVal = (ConstInitVal) constDef.getChildren().get(8);
			Array2D array = (Array2D) symbol;
			for (int i = 0; i < array.getShapeX(); i++) {
				for (int j = 0; j < array.getShapeY(); j++) {
					Value address = generateGetelementptr(
							symbol.getIRValue(),
							new ConstInt(0),
							new ConstInt(i),
							new ConstInt(j)
					);
					currentBasicBlock.add(address);
					ConstInitVal constInitVal2 = (ConstInitVal) constInitVal.getChildren().get(1 + 2 * i);
					ConstInitVal constInitVal3 = (ConstInitVal) constInitVal2.getChildren().get(1 + 2 * j);
					ConstExp constExp = (ConstExp) constInitVal3.getChildren().get(0);
					Value value = generateInstructionsFromConstExp(constExp);
					currentBasicBlock.add(generateStore(value, address));
				}
			}
		}
	}

	private void generateInstructionsFromVarDef(VarDef varDef) {
		LeafNode ident = (LeafNode) varDef.getChildren().get(0);
		Symbol symbol = ident.getSymbol();

		if (!(varDef.getChildren().get(varDef.getChildren().size() - 1) instanceof InitVal)) {
			// 未赋初值，由于已提前分配空间，无需生成指令
			return;
		}

		if (symbol instanceof Variable) {
			// 展开计算InitVal
			InitVal initVal = (InitVal) varDef.getChildren().get(2);
			Exp exp = (Exp) initVal.getChildren().get(0);
			Value value = generateInstructionsFromExp(exp);
			currentBasicBlock.add(generateStore(value, symbol.getIRValue()));
		} else if (symbol instanceof Array1D) {
			InitVal initVal = (InitVal) varDef.getChildren().get(5);
			Array1D array = (Array1D) symbol;
			for (int i = 0; i < array.getShape(); i++) {
				Value address = generateGetelementptr(
						symbol.getIRValue(),
						new ConstInt(0),
						new ConstInt(i)
				);
				currentBasicBlock.add(address);
				InitVal initVal2 = (InitVal) initVal.getChildren().get(1 + 2 * i);
				Exp exp = (Exp) initVal2.getChildren().get(0);
				Value value = generateInstructionsFromExp(exp);
				currentBasicBlock.add(generateStore(value, address));
			}
		} else {
			InitVal initVal = (InitVal) varDef.getChildren().get(8);
			Array2D array = (Array2D) symbol;
			for (int i = 0; i < array.getShapeX(); i++) {
				for (int j = 0; j < array.getShapeY(); j++) {
					Value address = generateGetelementptr(
							symbol.getIRValue(),
							new ConstInt(0),
							new ConstInt(i),
							new ConstInt(j)
					);
					currentBasicBlock.add(address);
					InitVal initVal2 = (InitVal) initVal.getChildren().get(1 + 2 * i);
					InitVal initVal3 = (InitVal) initVal2.getChildren().get(1 + 2 * j);
					Exp exp = (Exp) initVal3.getChildren().get(0);
					Value value = generateInstructionsFromExp(exp);
					currentBasicBlock.add(generateStore(value, address));
				}
			}
		}
	}

	private Value generateInstructionsFromConstExp(ConstExp constExp) {
		return generateInstructionsFromAddExp((AddExp) constExp.getChildren().get(0));
	}

	private Value generateInstructionsFromExp(Exp exp) {
		return generateInstructionsFromAddExp((AddExp) exp.getChildren().get(0));
	}

	private Value generateInstructionsFromAddExp(AddExp addExp) {
		if (addExp.getChildren().size() == 1) {
			MulExp mulExp = (MulExp) addExp.getChildren().get(0);
			return generateInstructionsFromMulExp(mulExp);
		}
		AddExp addExp1 = (AddExp) addExp.getChildren().get(0);
		LeafNode op = (LeafNode) addExp.getChildren().get(1);
		MulExp mulExp = (MulExp) addExp.getChildren().get(2);
		Value left = generateInstructionsFromAddExp(addExp1);
		Value right = generateInstructionsFromMulExp(mulExp);
		if (op.getToken().getType().equals(TokenType.PLUS)) {
			Add add = generateAdd(left, right);
			currentBasicBlock.add(add);
			return add;
		}
		Sub sub = generateSub(left, right);
		currentBasicBlock.add(sub);
		return sub;
	}

	private Value generateInstructionsFromMulExp(MulExp mulExp) {
		if (mulExp.getChildren().size() == 1) {
			UnaryExp unaryExp = (UnaryExp) mulExp.getChildren().get(0);
			return generateInstructionsFromUnaryExp(unaryExp);
		}
		MulExp mulExp1 = (MulExp) mulExp.getChildren().get(0);
		LeafNode op = (LeafNode) mulExp.getChildren().get(1);
		UnaryExp unaryExp = (UnaryExp) mulExp.getChildren().get(2);
		Value left = generateInstructionsFromMulExp(mulExp1);
		Value right = generateInstructionsFromUnaryExp(unaryExp);
		if (op.getToken().getType().equals(TokenType.MULT)) {
			Mul mul = generateMul(left, right);
			currentBasicBlock.add(mul);
			return mul;
		}
		if (op.getToken().getType().equals(TokenType.DIV)) {
			Sdiv sdiv = generateSdiv(left, right);
			currentBasicBlock.add(sdiv);
			return sdiv;
		}
		Srem srem = generateSrem(left, right);
		currentBasicBlock.add(srem);
		return srem;
	}

	private Value generateInstructionsFromUnaryExp(UnaryExp unaryExp) {
		if (unaryExp.getChildren().size() == 1) {
			// 子结点为PrimaryExp
			PrimaryExp primaryExp = (PrimaryExp) unaryExp.getChildren().get(0);
			return generateInstructionsFromPrimaryExp(primaryExp);
		}
		if (unaryExp.getChildren().size() == 2) {
			// 子结点为UnaryOp UnaryExp
			UnaryOp unaryOp = (UnaryOp) unaryExp.getChildren().get(0);
			LeafNode op = (LeafNode) unaryOp.getChildren().get(0);
			UnaryExp unaryExp1 = (UnaryExp) unaryExp.getChildren().get(1);
			Value operand = generateInstructionsFromUnaryExp(unaryExp1);
			if (op.getToken().getType().equals(TokenType.PLUS)) {
				// 正号忽略即可
				return operand;
			}
			if (op.getToken().getType().equals(TokenType.MINU)) {
				// 负号转换为 0 -
				Value left = new ConstInt(0);
				Sub sub = generateSub(left, operand);
				currentBasicBlock.add(sub);
				return sub;
			}
			// 将 ! 转为和 0 的比较，将结果转换为i32类型，以便可以连用 !
			Value right = new ConstInt(0);
			Eq eq = generateEq(operand, right);
			currentBasicBlock.add(eq);
			Zext zext = generateZext("i1", "i32", eq);
			currentBasicBlock.add(zext);
			return zext;
		}

		LeafNode ident = (LeafNode) unaryExp.getChildren().get(0);
		SymbolTable.Function function = (SymbolTable.Function) ident.getSymbol();

		ArrayList<Value> operands = new ArrayList<>();
		operands.add(function.getIRValue());
		if (unaryExp.getChildren().size() == 4) {
			FuncRParams funcRParams = (FuncRParams)  unaryExp.getChildren().get(2);
			operands.addAll(generateInstructionsFromFuncRParams(funcRParams));
		}
		Call call = generateCall(operands.toArray(new Value[0]));
		currentBasicBlock.add(call);
		return call;
	}

	private Value generateInstructionsFromPrimaryExp(PrimaryExp primaryExp) {
		if (primaryExp.getChildren().size() == 3) {
			// 子结点为 ( Exp )
			Exp exp = (Exp) primaryExp.getChildren().get(1);
			return generateInstructionsFromExp(exp);
		}
		ASTNode node = primaryExp.getChildren().get(0);
		if (node instanceof LVal) {
			return generateReadInstructionsFromLVal((LVal) node);
		}
		return generateInstructionsFromNumber((Number) node);
	}

	private Value generateReadInstructionsFromLVal(LVal lVal) {
		// 左值用于读取时的指令
		LeafNode ident = (LeafNode) lVal.getChildren().get(0);
		Symbol symbol = ident.getSymbol();
		if (symbol instanceof Variable) {
			if (((Variable) symbol).isGlobalConst()) {
				return new ConstInt(((Variable) symbol).getValue());
			}
			Load load = generateLoad(symbol.getIRValue());
			currentBasicBlock.add(load);
			return load;
		}

		int dimension;
		boolean isPointer;
		if (symbol instanceof Array1D) {
			dimension = 1;
			isPointer = ((Array1D) symbol).getShape() == 0;
		} else {
			dimension = 2;
			isPointer = ((Array2D) symbol).getShapeX() == 0;
		}

		if (isPointer) {
			// 转换指针的情况
			// 将指针读入
			Value address = generateLoad(symbol.getIRValue());
			currentBasicBlock.add(address);
			for (ASTNode node : lVal.getChildren()) {
				if (node instanceof Exp) {
					dimension--;
					Value exp = generateInstructionsFromExp((Exp) node);
					if (dimension == 0 && symbol instanceof Array2D) {
						address = generateGetelementptr(
								address, new ConstInt(0), exp
						);
					} else {
						address = generateGetelementptr(
								address, exp
						);
					}
					currentBasicBlock.add(address);
				}
			}
			if (dimension == 0) {
				// 将元素读入
				address = generateLoad(address);
				currentBasicBlock.add(address);
			} else if (address instanceof Getelementptr) {
				address = generateGetelementptr(
						address,
						new ConstInt(0),
						new ConstInt(0)
				);
				currentBasicBlock.add(address);
			}
			return address;
		} else {
			// 正常数组
			ArrayList<Value> exps = new ArrayList<>();
			for (ASTNode node : lVal.getChildren()) {
				if (node instanceof Exp) {
					Value exp = generateInstructionsFromExp((Exp) node);
					exps.add(exp);
				}
			}
			Value address = symbol.getIRValue();
			for (Value exp : exps) {
				address = generateGetelementptr(
						address,
						new ConstInt(0),
						exp
				);
				currentBasicBlock.add(address);
				dimension--;
			}
			if (dimension == 0) {
				// 将元素读入
				address = generateLoad(address);
				currentBasicBlock.add(address);
			} else {
				address = generateGetelementptr(
						address,
						new ConstInt(0),
						new ConstInt(0)
				);
				currentBasicBlock.add(address);
			}
			return address;
		}
	}

	private Value generateWriteInstructionsFromLVal(LVal lVal) {
		// 左值用于写入时的指令
		LeafNode ident = (LeafNode) lVal.getChildren().get(0);
		Symbol symbol = ident.getSymbol();
		if (symbol instanceof Variable) {
			return symbol.getIRValue();
		}
		boolean isPointer;
		if (symbol instanceof Array1D) {
			isPointer = ((Array1D) symbol).getShape() == 0;
		} else {
			isPointer = ((Array2D) symbol).getShapeX() == 0;
		}

		if (isPointer) {
			// 转换指针的情况
			// 将指针读入
			Value address = generateLoad(symbol.getIRValue());
			currentBasicBlock.add(address);
			int temp = 0;
			for (ASTNode node : lVal.getChildren()) {
				if (node instanceof Exp) {
					temp++;
					Value exp = generateInstructionsFromExp((Exp) node);
					if (temp == 2) {
						address = generateGetelementptr(
								address, new ConstInt(0), exp
						);
					} else {
						address = generateGetelementptr(
								address, exp
						);
					}
					currentBasicBlock.add(address);
				}
			}

			return address;
		} else {
			// 正常数组
			ArrayList<Value> exps = new ArrayList<>();
			for (ASTNode node : lVal.getChildren()) {
				if (node instanceof Exp) {
					Value exp = generateInstructionsFromExp((Exp) node);
					exps.add(exp);
				}
			}

			Value address = symbol.getIRValue();
			for (Value exp : exps) {
				address = generateGetelementptr(
						address,
						new ConstInt(0),
						exp
				);
				currentBasicBlock.add(address);
			}

			return address;
		}
	}

	private Value generateInstructionsFromNumber(Number number) {
		return new ConstInt(number.calculate());
	}

	private ArrayList<Value> generateInstructionsFromFuncRParams(FuncRParams funcRParams) {
		ArrayList<Value> values = new ArrayList<>();
		for (ASTNode node : funcRParams.getChildren()) {
			if (node instanceof Exp) {
				values.add(generateInstructionsFromExp((Exp) node));
			}
		}
		return values;
	}

	private void generateInstructionsFromCond(Cond cond) {
		LOrExp lOrExp = (LOrExp) cond.getChildren().get(0);
		generateInstructionsFromLOrExp(lOrExp, 0);
	}

	private void generateInstructionsFromLOrExp(LOrExp lOrExp, int nextType) {
		// 需要实现短路求值
		if (lOrExp.getChildren().size() == 1) {
			LAndExp lAndExp = (LAndExp) lOrExp.getChildren().get(0);
			generateInstructionsFromLAndExp(lAndExp, nextType);
		} else {
			LOrExp lOrExp1 = (LOrExp) lOrExp.getChildren().get(0);
			generateInstructionsFromLOrExp(lOrExp1, 1);

			LAndExp lAndExp = (LAndExp) lOrExp.getChildren().get(2);
			generateInstructionsFromLAndExp(lAndExp, nextType);
		}
	}

	private void generateInstructionsFromLAndExp(LAndExp lAndExp, int nextType) {
		// 需要实现短路求值
		if (lAndExp.getChildren().size() == 1) {
			EqExp eqExp = (EqExp) lAndExp.getChildren().get(0);
			generateBrFromEqExp(eqExp, nextType);
		} else {
			LAndExp lAndExp1 = (LAndExp) lAndExp.getChildren().get(0);
			generateInstructionsFromLAndExp(lAndExp1, 2);

			EqExp eqExp = (EqExp) lAndExp.getChildren().get(2);
			generateBrFromEqExp(eqExp, nextType);
		}
	}

	private void generateBrFromEqExp(EqExp eqExp, int nextType) {
		// 生成符合短路连接的跳转方式
		Value value = generateInstructionsFromEqExp(eqExp);
		Value zero = new ConstInt(0);
		Ne ne = generateNe(value, zero);
		currentBasicBlock.add(ne);

		BasicBlock nextCond = pregenerateBasicBlock();

		if (nextType == 2) {
			// 后面为&&，为假时短路，跳到else块
			currentBasicBlock.add(generateBr(ne, nextCond, basicBlocks[1]));
		} else if (nextType == 1) {
			// 后面||，为真时短路，跳到if块
			currentBasicBlock.add(generateBr(ne, basicBlocks[0], nextCond));
		} else {
			// 后面无表达式
			currentBasicBlock.add(generateBr(ne, basicBlocks[0], basicBlocks[1]));
		}
		if(nextType != 0) {
			currentBasicBlock = nextCond;
			currentFunction.addBasicBlock(currentBasicBlock);
			nameBasicBlock(currentBasicBlock);
		}
	}

	private Value generateInstructionsFromEqExp(EqExp eqExp) {
		if (eqExp.getChildren().size() == 1) {
			RelExp relExp = (RelExp) eqExp.getChildren().get(0);
			return generateInstructionsFromRelExp(relExp);
		}
		EqExp eqExp1 = (EqExp) eqExp.getChildren().get(0);
		LeafNode op = (LeafNode) eqExp.getChildren().get(1);
		RelExp relExp = (RelExp) eqExp.getChildren().get(2);
		Value left = generateInstructionsFromEqExp(eqExp1);
		Value right = generateInstructionsFromRelExp(relExp);
		Zext zext = null;
		switch (op.getToken().getType()) {
			case EQL:
				Eq eq = generateEq(left, right);
				currentBasicBlock.add(eq);
				zext = generateZext("i1", "i32", eq);
				currentBasicBlock.add(zext);
				break;
			case NEQ:
				Ne ne = generateNe(left, right);
				currentBasicBlock.add(ne);
				zext = generateZext("i1", "i32", ne);
				currentBasicBlock.add(zext);
		}
		return zext;
	}

	private Value generateInstructionsFromRelExp(RelExp relExp) {
		if (relExp.getChildren().size() == 1) {
			AddExp addExp = (AddExp) relExp.getChildren().get(0);
			return generateInstructionsFromAddExp(addExp);
		}
		RelExp relExp1 = (RelExp) relExp.getChildren().get(0);
		LeafNode op = (LeafNode) relExp.getChildren().get(1);
		AddExp addExp = (AddExp) relExp.getChildren().get(2);
		Value left = generateInstructionsFromRelExp(relExp1);
		Value right = generateInstructionsFromAddExp(addExp);
		Zext zext = null;
		switch (op.getToken().getType()) {
			case GRE:
				Gt gt = generateGt(left, right);
				currentBasicBlock.add(gt);
				zext = generateZext("i1", "i32", gt);
				currentBasicBlock.add(zext);
				break;
			case GEQ:
				Ge ge = generateGe(left, right);
				currentBasicBlock.add(ge);
				zext = generateZext("i1", "i32", ge);
				currentBasicBlock.add(zext);
				break;
			case LSS:
				Lt lt = generateLt(left, right);
				currentBasicBlock.add(lt);
				zext = generateZext("i1", "i32", lt);
				currentBasicBlock.add(zext);
				break;
			case LEQ:
				Le le = generateLe(left, right);
				currentBasicBlock.add(le);
				zext = generateZext("i1", "i32", le);
				currentBasicBlock.add(zext);
		}
		return zext;
	}

	//region 构建IR Value
	// 构建GlobalVariable
	private GlobalVariable generateGlobalVariable(Symbol symbol) {
		GlobalVariable globalVariable = new GlobalVariable("@" + symbol.getName(), symbol);
		symbol.setIRValue(globalVariable);
		return globalVariable;
	}

	// 构建Function
	private Function generateFunction(Symbol symbol) {
		Function function = new Function("@" + symbol.getName(), symbol);
		symbol.setIRValue(function);
		return function;
	}

	// 构建FunctionParam
	private FunctionParam generateFunctionParam(Symbol symbol) {
		String prefix = addPrefix ? "t" : "";
		FunctionParam value = new FunctionParam("%" + prefix + index, symbol);
		index++;
		return value;
	}

	// 构建BasicBlock
	private BasicBlock generateBasicBlock() {
		String prefix = addPrefix ? "l" : "";
		BasicBlock basicBlock = new BasicBlock(prefix + index);
		index++;
		return basicBlock;
	}

	// 预构建BasicBlock
	private BasicBlock pregenerateBasicBlock() {
		return new BasicBlock("");
	}

	// 为预构建的BasicBlock编号
	private void nameBasicBlock(BasicBlock basicBlock) {
		String prefix = addPrefix ? "l" : "";
		basicBlock.setName(prefix + index);
		index++;
	}

	// 构建Alloca
	private Alloca generateAlloca(Symbol symbol) {
		String prefix = addPrefix ? "t" : "";
		Alloca alloca = new Alloca("%" + prefix + index);
		index++;
		alloca.setSymbol(symbol);
		symbol.setIRValue(alloca);
		return alloca;
	}

	// 构建Store
	private Store generateStore(Value...operands) {
		return new Store(operands);
	}

	// 构建Load
	private Load generateLoad(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Load load = new Load("%" + prefix + index, operands);
		index++;
		return load;
	}

	// 构建Add
	private Add generateAdd(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Add add = new Add("%" + prefix + index, operands);
		index++;
		return add;
	}

	// 构建Sub
	private Sub generateSub(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Sub sub = new Sub("%" + prefix + index, operands);
		index++;
		return sub;
	}

	// 构建Mul
	private Mul generateMul(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Mul mul = new Mul("%" + prefix + index, operands);
		index++;
		return mul;
	}

	// 构建Sdiv
	private Sdiv generateSdiv(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Sdiv sdiv = new Sdiv("%" + prefix + index, operands);
		index++;
		return sdiv;
	}

	// 构建Srem
	private Srem generateSrem(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Srem srem = new Srem("%" + prefix + index, operands);
		index++;
		return srem;
	}

	// 构建Eq
	private Eq generateEq(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Eq eq = new Eq("%" + prefix + index, operands);
		index++;
		return eq;
	}

	// 构建Ne
	private Ne generateNe(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Ne ne = new Ne("%" + prefix + index, operands);
		index++;
		return ne;
	}

	// 构建Lt
	private Lt generateLt(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Lt lt = new Lt("%" + prefix + index, operands);
		index++;
		return lt;
	}

	// 构建Le
	private Le generateLe(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Le le = new Le("%" + prefix + index, operands);
		index++;
		return le;
	}

	// 构建Gt
	private Gt generateGt(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Gt gt = new Gt("%" + prefix + index, operands);
		index++;
		return gt;
	}

	// 构建Gt
	private Ge generateGe(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Ge ge = new Ge("%" + prefix + index, operands);
		index++;
		return ge;
	}

	// 构建Ret
	private Ret generateRet(Value...operands) {
		return new Ret(operands);
	}

	// 构建Call
	private Call generateCall(Value...operands) {
		Value function = operands[0];
		SymbolTable.Function symbol = (SymbolTable.Function) function.getSymbol();
		if (symbol.hasReturn()) {
			String prefix = addPrefix ? "t" : "";
			Call call = new Call("%" + prefix + index, operands);
			index++;
			return call;
		}
		return new Call(operands);
	}

	private GetInt generateGetInt() {
		String prefix = addPrefix ? "t" : "";
		GetInt getInt = new GetInt("%" + prefix + index);
		index++;
		return getInt;
	}

	private ConstString generateConstString(String value) {
		String name = "@.str" + (constStrings.isEmpty() ? "" : "." + constStrings.size());
		return new ConstString(name, value);
	}

	private Putstr generatePutstr(Value operand) {
		return new Putstr(operand);
	}

	private Putint generatePutint(Value operand) {
		return new Putint(operand);
	}

	private Br generateBr(Value...operands) {
		return new Br(operands);
	}

	private Zext generateZext(String sourceType, String targetType, Value operand) {
		String prefix = addPrefix ? "t" : "";
		Zext zext = new Zext("%" + prefix + index, sourceType, targetType, operand);
		index++;
		return zext;
	}

	private Getelementptr generateGetelementptr(Value...operands) {
		String prefix = addPrefix ? "t" : "";
		Getelementptr getelementptr = new Getelementptr("%" + prefix + index, operands);
		index++;
		return getelementptr;
	}
	//endregion
}
