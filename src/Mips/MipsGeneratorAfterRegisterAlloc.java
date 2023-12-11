package Mips;

import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import Optimizer.Mips.MipsOptimizer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class MipsGeneratorAfterRegisterAlloc {
	private final IrModule module;
	private final BufferedWriter writer;
	private Set<String> globalRegisterUsed; // 可用寄存器
	private Value[] tempRegisterUsed = new Value[10];
	private LinkedHashMap<Value, Integer> tempRegisterUseMap = new LinkedHashMap<>();
	private Map<Function, Map<Value, String>> registers;
	private Map<Function, Set<Value>> spills;
	// 栈指针虚拟位置，初始时为0，仅用于计算偏移
	private int sp = 0;
	private Function currentFunction;

	public MipsGeneratorAfterRegisterAlloc(IrModule module, BufferedWriter writer) {
		this.module = module;
		this.writer = writer;
	}

	public void generate() throws IOException {
		// 进行优化，消除phi指令
		MipsOptimizer optimizer = new MipsOptimizer();
		optimizer.optimize(module);
		registers = optimizer.getRegisters();
		spills = optimizer.getSpills();
		generateMipsFromModule();
	}

	public void generateMipsFromModule() throws IOException {
		// 生成全局数据段
		writer.write(".data");
		writer.newLine();
		for (GlobalVariable var : module.getGlobalVariables()) {
			writer.write(var.getMips());
			writer.newLine();
			var.setAddr(var.getRawName());
		}

		for (ConstString str: module.getConstStrings()) {
			writer.write(str.getMips());
			writer.newLine();
			str.setAddr(str.getRawName());
		}

		// 添加宏
		generateMipsForLibFunction();

		writer.write(".text");
		writer.newLine();
		writer.write("jal main");
		writer.newLine();
		writer.write("move $a0, $v0");
		writer.newLine();
		writer.write("li $v0, 10");
		writer.newLine();
		writer.write("syscall");
		writer.newLine();

		for (Function func : module.getFunctions()) {
			generateMipsFromFunction(func);
		}
	}

	public void generateMipsForLibFunction() throws IOException {
		// getint
		writer.write(".macro getint");
		writer.newLine();
		writer.write("li $v0, 5");
		writer.newLine();
		writer.write("syscall");
		writer.newLine();
		writer.write(".end_macro");
		writer.newLine();

		// putint
		writer.write(".macro putint");
		writer.newLine();
		writer.write("li $v0, 1");
		writer.newLine();
		writer.write("syscall");
		writer.newLine();
		writer.write(".end_macro");
		writer.newLine();

		// putch
		writer.write(".macro putch");
		writer.newLine();
		writer.write("li $v0, 11");
		writer.newLine();
		writer.write("syscall");
		writer.newLine();
		writer.write(".end_macro");
		writer.newLine();

		// putstr
		writer.write(".macro putstr");
		writer.newLine();
		writer.write("li $v0, 4");
		writer.newLine();
		writer.write("syscall");
		writer.newLine();
		writer.write(".end_macro");
		writer.newLine();
	}

	public void generateMipsFromFunction(Function function) throws IOException {
		currentFunction = function;
		// 记录初始$sp位置
		function.setPrimarySp(sp);
		writer.write(function.getRawName() + ":");
		writer.newLine();

		// 计算函数参数地址
		for (int i = function.getParams().size() - 1, offset = 0; i >=0 ; i--) {
			function.getParams().get(i).setAddr(offset + sp + "");
			offset += 4; // 参数大小均为4字节
		}

		// 保存$ra、$s0-$s7
		sp -= 4;
		function.setRaAddr(sp);
		Map<String, Integer> registerAddr = new HashMap<>();
		if (!function.getName().equals("@main")) {
			globalRegisterUsed = new HashSet<>(registers.get(function).values());
			globalRegisterUsed.remove("$a0");
			globalRegisterUsed.remove("$a1");
			globalRegisterUsed.forEach(r->{
				sp -= 4;
				registerAddr.put(r, sp);
			});
			function.setRegisterAddr(registerAddr);
		}

		allocStack(function.getPrimarySp() - sp);

		saveToStack("$ra", function.getRaAddr() - sp);
		if (!function.getName().equals("@main")) {
			for (String reg : globalRegisterUsed) {
				saveToStack(reg, registerAddr.get(reg) - sp);
			}
		}

		// 为alloca分配空间
		int primary = sp;
		for (BasicBlock b : currentFunction.getBasicBlocks()) {
			for (Value v : b.getInstructions()) {
				if (v instanceof Alloca) {
					sp -= ((Alloca) v).getSize();
					v.setAddr("" + sp);
				}
			}
		}
		// 为溢出的临时变量分配空间
		for (Value value : spills.get(function)) {
			sp -= 4;
			value.setAddr("" + sp);
		}
		allocStack(primary - sp);

		for (BasicBlock block : function.getBasicBlocks()) {
			generateMipsFromBasicBlock(block);
		}
		// 恢复$ra等、释放栈空间在生成jr $ra的函数中进行

		sp = function.getPrimarySp();
		freeAllTempRegistersWithoutStore();
	}

	public void generateMipsFromBasicBlock(BasicBlock block) throws IOException {
		// 输出标签
		if (!block.getUseList().isEmpty()) {
			writer.write(block.getMipsLabel(currentFunction.getRawName()) + ": ");
			writer.newLine();
		}

		ArrayList<Instruction> instructions = block.getInstructions();
		for (Instruction instruction : instructions) {
			if (instruction instanceof Alloca) {
				continue;
			} else if (instruction instanceof Store) {
				generateMipsFromStore((Store) instruction);
			} else if (instruction instanceof Load) {
				generateMipsFromLoad((Load) instruction);
			} else if (instruction instanceof Getelementptr) {
				generateMipsFromGetelementptr((Getelementptr) instruction);
			} else if (instruction instanceof Add) {
				generateMipsFromAdd((Add) instruction);
			} else if (instruction instanceof Sub) {
				generateMipsFromSub((Sub) instruction);
			} else if (instruction instanceof Mul) {
				generateMipsFromMul((Mul) instruction);
			} else if (instruction instanceof Sdiv) {
				generateMipsFromSdiv((Sdiv) instruction);
			} else if (instruction instanceof Srem) {
				generateMipsFromSrem((Srem) instruction);
			} else if (instruction instanceof Gt) {
				generateMipsFromGt((Gt) instruction);
			} else if (instruction instanceof Ge) {
				generateMipsFromGe((Ge) instruction);
			} else if (instruction instanceof Lt) {
				generateMipsFromLt((Lt) instruction);
			} else if (instruction instanceof Le) {
				generateMipsFromLe((Le) instruction);
			} else if (instruction instanceof Eq) {
				generateMipsFromEq((Eq) instruction);
			} else if (instruction instanceof Ne) {
				generateMipsFromNe((Ne) instruction);
			} else if (instruction instanceof Zext) {
				generateMipsFromZext((Zext) instruction);
			} else if (instruction instanceof Call) {
				generateMipsFromCall((Call) instruction);
			} else if (instruction instanceof GetInt) {
				generateMipsFromGetInt((GetInt) instruction);
			} else if (instruction instanceof Putint) {
				generateMipsFromPutint((Putint) instruction);
			} else if (instruction instanceof Putstr) {
				generateMipsFromPutstr((Putstr) instruction);
			} else if (instruction instanceof Ret) {
				generateMipsFromRet((Ret) instruction);
			} else if (instruction instanceof Br) {
				generateMipsFromBr((Br) instruction);
			} else if (instruction instanceof Move) {
				generateMipsFromMove((Move) instruction);
			} else if (instruction instanceof PushStack) {
				generateMipsFromPushStack((PushStack) instruction);
			}
		}
		if (!(instructions.get(instructions.size() - 1) instanceof Br)
				&& !(instructions.get(instructions.size() - 1) instanceof Ret)) {
			// Br被优化了
			freeAllTempRegisters();
		}
	}

	private void generateMipsFromStore(Store store) throws IOException {
		// 获取操作数的寄存器
		Value value = store.getOperands().get(0);
		String reg1 = getRegister(value);

		Value address = store.getOperands().get(1);
		if (address instanceof GlobalVariable) {
			String label = address.getRawName();
			writer.write(String.format("sw %s, %s", reg1, label));
			writer.newLine();
		} else if (address instanceof Getelementptr) {
			// 地址由getelementptr计算出
			String reg2 = getRegister(address);
			writer.write(String.format("sw %s, (%s)", reg1, reg2));
			writer.newLine();
		} else if (address instanceof Alloca) {
			// 必然是普通变量，地址为数值形式
			int addr = Integer.parseInt(address.getAddr());
			int offset = addr - sp;
			writer.write(String.format("sw %s, %d($sp)", reg1, offset));
			writer.newLine();
		}
	}

	private void generateMipsFromLoad(Load load) throws IOException {
		Value address = load.getOperands().get(0);
		if (address instanceof GlobalVariable) {
			String label = address.getRawName();
			// 为结果分配寄存器
			String reg1 = getRegister(load);
			writer.write(String.format("lw %s, %s", reg1, label));
			writer.newLine();
		} else if (address instanceof Getelementptr || address instanceof Load) {
			// 地址由getelementptr计算出
			String reg2 = getRegister(address);
			String reg1 = getRegister(load);
			writer.write(String.format("lw %s, (%s)", reg1, reg2));
			writer.newLine();
		} else if (address instanceof Instruction || address instanceof FunctionParam) {
			int addr = Integer.parseInt(address.getAddr());
			int offset = addr - sp;
			String reg1 = getRegister(load);
			writer.write(String.format("lw %s, %d($sp)", reg1, offset));
			writer.newLine();
		}
	}
	private void generateMipsFromGetelementptr(Getelementptr g) throws IOException {
		// 由于该指令需要分步计算，采用$a2,$a3暂时记录中间结果
		ArrayList<Value> operands = g.getOperands();
		Value address = operands.get(0);
		String reg0 = getRegister(g);
		if (address instanceof GlobalVariable) {
			// 全局数组，初始地址为label
			writer.write(String.format("la $a2, %s", address.getRawName()));
			writer.newLine();
		} else if (address instanceof Alloca) {
			// 局部数组，起始地址为$sp+offset
			int addr = Integer.parseInt(address.getAddr());
			writer.write(String.format("addiu $a2, $sp, %d", addr - sp));
			writer.newLine();
		} else {
			// getelementptr或指针值
			String reg1 = getRegister(address);
			writer.write(String.format("move $a2, %s", reg1));
			writer.newLine();
		}

		for (int i = 1; i < operands.size(); i++) {
			int layerSize = g.getSize(i - 1);
			String resultReg = i == operands.size() - 1 ? reg0 : "$a2";
			if (operands.get(i) instanceof ConstInt) {
				int offset = layerSize * ((ConstInt) operands.get(i)).getValue();
				writer.write(String.format("addiu %s, $a2, %d", resultReg, offset));
				writer.newLine();
			} else {
				String reg1 = getRegister(operands.get(i));
				// 中间临时结果使用 $a3 暂存
				writer.write(String.format("mul $a3, %s, %d", reg1, layerSize));
				writer.newLine();
				writer.write(String.format("addu %s, $a2, $a3", resultReg));
				writer.newLine();
			}
		}
	}

	private void generateMipsFromAdd(Add a) throws IOException {
		calculate(a, "addu");
	}

	private void generateMipsFromSub(Sub s) throws IOException {
		calculate(s, "subu");
	}

	private void generateMipsFromMul(Mul m) throws IOException {
		calculate(m, "mul");
	}

	private void generateMipsFromSdiv(Sdiv s) throws IOException {
		calculate(s, "div");
	}

	private void generateMipsFromSrem(Srem s) throws IOException {
		calculate(s, "rem");
	}

	private void generateMipsFromGt(Gt g) throws IOException {
		calculate(g, "sgt");
	}

	private void generateMipsFromGe(Ge g) throws IOException {
		calculate(g, "sge");
	}

	private void generateMipsFromLt(Lt l) throws IOException {
		calculate(l, "slt");
	}

	private void generateMipsFromLe(Le l) throws IOException {
		calculate(l, "sle");
	}

	private void generateMipsFromEq(Eq e) throws IOException {
		calculate(e, "seq");
	}

	private void generateMipsFromNe(Ne n) throws IOException {
		calculate(n, "sne");
	}

	private void calculate(Instruction instruction, String op) throws IOException {
		// 左操作数必须获取寄存器
		Value left = instruction.getOperands().get(0);
		String reg0 = getRegister(instruction);
		String reg1 = getRegister(left);

		Value right = instruction.getOperands().get(1);
		if (right instanceof ConstInt && !op.equals("slt")) {
			// 右操作数为常量，无需获取寄存器
			int val = ((ConstInt) right).getValue();
			if (op.equals("addu")) {
				writer.write(String.format("addiu %s, %s, %d", reg0, reg1, val));
				writer.newLine();
			} else if (op.equals("subu")) {
				writer.write(String.format("addiu %s, %s, %d", reg0, reg1, -val));
				writer.newLine();
			} else if (op.equals("mul") && canDoMulOptimize(val)) {
				// 乘法优化
				mulOptimize((Mul) instruction);
			} else if (op.equals("div")) {
				// 除法优化
				divOptimize((Sdiv) instruction);
			} else {
				writer.write(String.format(op + " %s, %s, %d", reg0, reg1, val));
				writer.newLine();
			}
		} else if (op.equals("div")) {
			// 对于伪指令div $t1, $t2, $t3，Mars会生成检查除数不为零的语句，不需要
			String reg2 = getRegister(right);
			writer.write(String.format("div %s, %s", reg1, reg2));
			writer.newLine();
			writer.write(String.format("mflo %s", reg0));
			writer.newLine();
		} else if (op.equals("rem")) {
			String reg2 = getRegister(right);
			writer.write(String.format("div %s, %s", reg1, reg2));
			writer.newLine();
			writer.write(String.format("mfhi %s", reg0));
			writer.newLine();
		} else {
			// 其他情况，需要获取寄存器
			String reg2 = getRegister(right);
			writer.write(String.format(op + " %s, %s, %s", reg0, reg1, reg2));
			writer.newLine();
		}
	}

	private boolean canDoMulOptimize(int value) {
		boolean isNeg = value < 0;
		if (isNeg) {
			value = -value;
		}
		long a = 1;
		while (a <= value) {
			a *= 2;
		}
		if (a - value <= (isNeg ? 1 : 2)) {
			return true;
		}
		a /= 2;
		return isNeg ? value - a <= 1 : value - a <= 2;
	}

	private void mulOptimize(Mul mul) throws IOException {
		Value left = mul.getOperands().get(0);
		Value right = mul.getOperands().get(1);
		int val = ((ConstInt) right).getValue();
		String reg0 = getRegister(mul);
		String reg1 = getRegister(left);
		if (val == 0) {
			writer.write(String.format("li %s, 0", reg0));
			writer.newLine();
			return;
		} else if (val == 1) {
			writer.write(String.format("move %s, %s", reg0, reg1));
			writer.newLine();
			return;
		} else if (val == -1) {
			writer.write(String.format("subu %s, $0, %s", reg0, reg1));
			writer.newLine();
			return;
		}
		int abs = val < 0 ? -val : val;
		int a = 0;
		while ((1L << a) <= abs) {
			a++;
		}
		int subCount = (int) ((1L << a) - abs);
		a--;
		int addCount = abs - (1 << a);
		if (addCount == 0) {
			writer.write(String.format("sll %s, %s, %d", reg0, reg1, a));
			writer.newLine();
		} else if (addCount <= subCount) {
			writer.write(String.format("sll $a2, %s, %d", reg1, a));
			writer.newLine();

			for (int i = 0; i < addCount; i++) {
				if (i == addCount - 1) {
					writer.write(String.format("addu %s, $a2, %s", reg0, reg1));
					writer.newLine();
				} else {
					writer.write(String.format("addu $a2, $a2, %s", reg1));
					writer.newLine();
				}
			}
		} else {
			writer.write(String.format("sll $a2, %s, %d", reg1, a + 1));
			writer.newLine();

			for (int i = 0; i < subCount; i++) {
				if (i == subCount - 1) {
					writer.write(String.format("subu %s, $a2, %s", reg0, reg1));
					writer.newLine();
				} else {
					writer.write(String.format("subu $a2, $a2, %s", reg1));
					writer.newLine();
				}
			}
		}

		if (val < 0) {
			writer.write(String.format("subu %s, $0, %s", reg0, reg0));
			writer.newLine();
		}
	}

	private void divOptimize(Sdiv sdiv) throws IOException {
		Value left = sdiv.getOperands().get(0);
		Value right = sdiv.getOperands().get(1);
		int val = ((ConstInt) right).getValue();
		String reg0 = getRegister(sdiv);
		String reg1 = getRegister(left);
		if (val == 1) {
			writer.write(String.format("move %s, %s", reg0, reg1));
			writer.newLine();
			return;
		} else if (val == -1) {
			writer.write(String.format("subu %s, $0, %s", reg0, reg1));
			writer.newLine();
			return;
		}

		int abs = val >= 0 ? val : -val;
		if ((abs & (abs - 1)) == 0) {
			// (n + ((n >> 31) >>> (32 - l))) >> l
			writer.write(String.format("sra $a2, %s, 31", reg1));
			writer.newLine();
			int l = getCTZ(abs);
			writer.write(String.format("srl $a2, $a2, %d", 32 - l));
			writer.newLine();
			writer.write(String.format("addu $a2, $a2, %s", reg1));
			writer.newLine();
			writer.write(String.format("sra %s, $a2, %d", reg0, l));
			writer.newLine();
		} else {
			long[] multiplier = chooseMultiplier(abs, 31);
			long m = multiplier[0];
			long sh = multiplier[1];
			if (m < 2147483648L) {
				writer.write(String.format("li $a2, %d", m));
				writer.newLine();
				writer.write(String.format("mult %s, $a2", reg1));
				writer.newLine();
				writer.write("mfhi $a3");
				writer.newLine();
			} else {
				writer.write(String.format("li $a2, %d", m - (1L << 32)));
				writer.newLine();
				writer.write(String.format("mult %s, $a2", reg1));
				writer.newLine();
				writer.write("mfhi $a3");
				writer.newLine();
				writer.write(String.format("addu $a3, $a3, %s", reg1));
				writer.newLine();
			}
			writer.write(String.format("sra $a3, $a3, %d", sh));
			writer.newLine();
			writer.write(String.format("srl $a2, %s, 31", reg1));
			writer.newLine();
			writer.write(String.format("addu %s, $a3, $a2", reg0));
			writer.newLine();
		}

		if (val < 0) {
			writer.write(String.format("subu %s, $0, %s", reg0, reg0));
			writer.newLine();
		}
	}

	private int getCTZ(int num) {
		int r = 0;
		num >>>= 1;
		while (num > 0) {
			r++;
			num >>>= 1;
		}
		return r; // 0 - 31
	}

	private long[] chooseMultiplier(int d, int prec) {
		long nc = (1L << prec) - ((1L << prec) % d) - 1;
		long p = 32;
		while ((1L << p) <= nc * (d - (1L << p) % d)) {
			p++;
		}
		long m = (((1L << p) + (long) d - (1L << p) % d) / (long) d);
		long n = ((m << 32) >>> 32);
		return new long[]{n, p - 32, 0L};
	}

	private void generateMipsFromZext(Zext zext) throws IOException {
		// 由于只涉及i1到i32的转换，实际无需进行，
		// 但涉及寄存器的分配，需要进行一次move
		Value operand = zext.getOperands().get(0);
		String reg1 = getRegister(operand);
		String reg0 = getRegister(zext);

		if (reg0.equals(reg1)) {
			return;
		}
		writer.write(String.format("move %s, %s", reg0, reg1));
		writer.newLine();
	}

	private void generateMipsFromCall(Call call) throws IOException {
		// 函数调用
		ArrayList<Value> operands = call.getOperands();
		Function function = (Function) operands.get(0);

		// 参数压栈已由ToStack指令进行
		// 保存临时寄存器
		freeAllTempRegisters();

		// 跳转
		writer.write(String.format("jal %s", function.getRawName()));
		writer.newLine();

		// 有返回值时，保存返回值至寄存器
		if (((SymbolTable.Function)function.getSymbol()).hasReturn()) {
			String reg = getRegister(call);
			writer.write("move " + reg + ", $v0");
			writer.newLine();
		}
		// 参数占用的栈空间需释放
		freeStack(function.getParams().size() * 4);
		sp += function.getParams().size() * 4;
	}

	private void generateMipsFromGetInt(GetInt getInt) throws IOException {
		// 无需保存寄存器
		// 调用宏
		writer.write("getint");
		writer.newLine();

		// 保存$v0的值至寄存器
		String reg = getRegister(getInt);
		writer.write(String.format("move %s, $v0", reg));
		writer.newLine();
	}

	private void generateMipsFromPutint(Putint putint) throws IOException {
		// 仅用到$a0、$V0，无需保存

		// 传递参数
		Value operand = putint.getOperands().get(0);
		String reg = getRegister(operand);
		writer.write(String.format("move $a0, %s", reg));
		writer.newLine();

		// 调用宏
		writer.write("putint");
		writer.newLine();
	}

	private void generateMipsFromPutstr(Putstr putstr) throws IOException {
		// 仅用到$a0、$V0，无需保存

		// 传递参数
		Value operand = putstr.getOperands().get(0);
		writer.write(String.format("la $a0, %s", operand.getAddr()));
		writer.newLine();

		// 调用宏
		writer.write("putstr");
		writer.newLine();
	}

	private void generateMipsFromRet(Ret r) throws IOException {
		if (!r.getOperands().isEmpty()) {
			// 返回值存入$v0
			Value value = r.getOperands().get(0);
			if (value instanceof ConstInt){
				writer.write(String.format("li $v0, %d", ((ConstInt) value).getValue()));
				writer.newLine();
			} else {
				String reg = getRegister(value);
				writer.write(String.format("move $v0, %s", reg));
				writer.newLine();
			}
		}

		// 恢复$ra等
		loadFromStack("$ra", currentFunction.getRaAddr() - sp);
		if (!currentFunction.getName().equals("@main")) {
			for (String reg : globalRegisterUsed) {
				loadFromStack(reg, currentFunction.getRegisterAddr().get(reg) - sp);
			}
		}
		// 释放栈空间
		freeStack(currentFunction.getPrimarySp() - sp);
		// 函数返回语句也为跳转语句，需释放临时寄存器，但不需存回栈空间
		freeAllTempRegistersWithoutStore();

		// 返回
		writer.write("jr $ra");
		writer.newLine();
	}

	private void generateMipsFromBr(Br b) throws IOException {
		ArrayList<Value> operands = b.getOperands();
		if (operands.size() == 1) {
			BasicBlock block = (BasicBlock) operands.get(0);
			// 离开基本块前，释放寄存器
			freeAllTempRegisters();
			writer.write("j " + block.getMipsLabel(currentFunction.getRawName()));
			writer.newLine();
		} else {
			Value cond = operands.get(0);
			BasicBlock trueBlock = (BasicBlock) operands.get(1);
			BasicBlock falseBlock = (BasicBlock) operands.get(2);

			String reg = getRegister(cond);
			// 离开基本块前，释放临时寄存器
			freeAllTempRegisters();
			writer.write(String.format("beqz %s, %s", reg, falseBlock.getMipsLabel(currentFunction.getRawName())));
			writer.newLine();
			writer.write(String.format("j %s", trueBlock.getMipsLabel(currentFunction.getRawName())));
			writer.newLine();
		}
	}

	private void generateMipsFromMove(Move move) throws IOException {
		Value left = move.getOperands().get(0);
		Value right = move.getOperands().get(1);
		if (right instanceof ConstInt) {
			String reg0 = getRegister(left);
			writer.write(String.format("li %s, %d", reg0, ((ConstInt) right).getValue()));
			writer.newLine();
			return;
		}
		String reg0 = getRegister(left);
		String reg1 = getRegister(right);
		if (reg0.equals(reg1)) {
			// 无需move
			return;
		}
		writer.write(String.format("move %s, %s", reg0, reg1));
		writer.newLine();
	}

	// 为压栈命令生成mips
	private void generateMipsFromPushStack(PushStack p) throws IOException {
		sp -= 4;
		allocStack(4);
		String reg = getRegister(p.getOperands().get(0));
		saveToStack(reg, 0);
	}

	private void allocStack(int size) throws IOException {
		if (size == 0) {
			return;
		}
		// 分配栈空间
		writer.write("addiu $sp, $sp, " + (-size));
		writer.newLine();
	}

	private void freeStack(int size) throws IOException {
		if (size == 0) {
			return;
		}
		// 释放栈空间
		writer.write("addiu $sp, $sp, " + size);
		writer.newLine();
	}

	private void saveToStack(String reg, int offset) throws IOException {
		writer.write(String.format("sw %s, %d($sp)", reg, offset));
		writer.newLine();
	}

	private void loadFromStack(String reg, int offset) throws IOException {
		writer.write(String.format("lw %s, %d($sp)", reg, offset));
		writer.newLine();
	}

	// 用于获取操作数（中间变量）所在的寄存器
	private String getRegister(Value value) throws IOException {
		String reg = registers.get(currentFunction).get(value);
		if (value instanceof ConstInt) {
			// 如果是数值字面量，需要先li
			writer.write(String.format("li %s, %d", reg, ((ConstInt) value).getValue()));
			writer.newLine();
		} else if (reg == null) {
			reg = "$t" + getTempRegister(value);
		}
		return reg;
	}

	private int allocTempRegister(Value value) throws IOException {
		for (int i = 0; i < 10; i++) {
			if (tempRegisterUsed[i] == null) {
				// 记录分配情况
				tempRegisterUsed[i] = value;
				tempRegisterUseMap.put(value, i);
				return i;
			}
		}

		// 无空闲寄存器，需要释放
		// 释放未访问时间最长的
		Value v = tempRegisterUseMap.keySet().iterator().next();
		int reg = tempRegisterUseMap.get(v);
		freeTempRegister(v);

		// 记录分配情况
		tempRegisterUsed[reg] = value;
		tempRegisterUseMap.put(value, reg);

		return reg;
	}

	// 用于获取操作数（中间变量）所在的寄存器
	private int getTempRegister(Value value) throws IOException {
		if (tempRegisterUseMap.containsKey(value)) {
			// 获取后更新访问时间
			int reg = tempRegisterUseMap.get(value);
			tempRegisterUseMap.remove(value);
			tempRegisterUseMap.put(value, reg);
			return reg;
		}

		// 分配寄存器并读入
		int reg = allocTempRegister(value);

		loadFromStack("$t" + reg, Integer.parseInt(value.getAddr()) - sp);
		return reg;
	}

	private void freeTempRegister(Value value) throws IOException {
		int reg = tempRegisterUseMap.get(value);
		if (value instanceof Instruction) {
			// 保存至栈
			saveToStack("$t" + reg, Integer.parseInt(value.getAddr()) - sp);
		}
		tempRegisterUsed[reg] = null;
		tempRegisterUseMap.remove(value);
	}

	// 释放临时寄存器，将值存回栈空间
	private void freeAllTempRegisters() throws IOException {
		for (Value value : tempRegisterUseMap.keySet()) {
			int reg = tempRegisterUseMap.get(value);
			if (tempRegisterUsed[reg] instanceof Instruction){
				// 保存至栈
				saveToStack("$t" + reg, Integer.parseInt(tempRegisterUsed[reg].getAddr()) - sp);
			}
			tempRegisterUsed[reg] = null;
		}
		tempRegisterUseMap.clear();
	}

	private void freeAllTempRegistersWithoutStore() {
		tempRegisterUsed = new Value[10];
		tempRegisterUseMap.clear();
	}
}
