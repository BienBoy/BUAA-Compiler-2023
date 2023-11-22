package Mips;

import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import Optimizer.Mips.MipsOptimizer;
import Optimizer.Mips.ReplacePhi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class MipsGeneratorAfterRegisterAlloc {
	private final IrModule module;
	private final BufferedWriter writer;
	private final Set<String> registerAvailable = new HashSet<String>(){{
		add("$t0");add("$t1");add("$t2");add("$t3");add("$t4");add("$t5");
		add("$t6");add("$t7");add("$t8");add("$t9");add("$s0");add("$s1");
		add("$s2");add("$s3");add("$s4");add("$s5");add("$s6");add("$s7");
	}}; // 可用寄存器
	Map<Value, String> registers;
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

		// 保存$ra、$t0-$t9、$s0-$s7
		sp -= 4;
		function.setRaAddr(sp);
		Map<String, Integer> registerAddr = new HashMap<>();
		registerAvailable.forEach(r->{
			sp -= 4;
			registerAddr.put(r, sp);
		});
		function.setRegisterAddr(registerAddr);

		allocStack(function.getPrimarySp() - sp);

		saveToStack("$ra", function.getRaAddr() - sp);
		for (String reg : registerAvailable) {
			saveToStack(reg, registerAddr.get(reg) - sp);
		}

		// 为所有的中间变量分配空间
		int primary = sp;
		for (BasicBlock b : currentFunction.getBasicBlocks()) {
			for (Value v : b.getValues()) {
				if (v instanceof Alloca) {
					sp -= ((Alloca) v).getSize();
					v.setAddr("" + sp);
				}
			}
		}
		allocStack(primary - sp);

		for (BasicBlock block : function.getBasicBlocks()) {
			generateMipsFromBasicBlock(block);
		}
		// 恢复$ra等、释放栈空间在生成jr $ra的函数中进行

		sp = function.getPrimarySp();
	}

	public void generateMipsFromBasicBlock(BasicBlock block) throws IOException {
		// 输出标签
		if (!block.getUseList().isEmpty()) {
			writer.write(block.getMipsLabel(currentFunction.getRawName()) + ": ");
			writer.newLine();
		}

		ArrayList<Instruction> instructions = block.getValues();
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
		} else if (address instanceof Getelementptr) {
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
		// 由于该指令需要分步计算，采用$v0,$v1暂时记录中间结果
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
			writer.write(String.format("add $a2, $sp, %d", addr - sp));
			writer.newLine();
		} else {
			// getelementptr或指针值
			String reg1 = getRegister(address);
			writer.write(String.format("move $a2, %s", reg1));
			writer.newLine();
		}

		for (int i = 1; i < operands.size(); i++) {
			int layerSize = g.getSize(i - 1);
			if (operands.get(i) instanceof ConstInt) {
				int offset = layerSize * ((ConstInt) operands.get(i)).getValue();
				writer.write(String.format("add $a2, $a2, %d", offset));
				writer.newLine();
			} else {
				String reg1 = getRegister(operands.get(i));
				// 中间临时结果使用 $fp 暂存
				writer.write(String.format("mul $a3, %s, %d", reg1, layerSize));
				writer.newLine();
				writer.write("add $a2, $a2, $a3");
				writer.newLine();
			}
		}
		writer.write(String.format("move %s, $a2", reg0));
		writer.newLine();
	}

	private void generateMipsFromAdd(Add a) throws IOException {
		calculate(a, "add");
	}

	private void generateMipsFromSub(Sub s) throws IOException {
		calculate(s, "sub");
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
		// 命令右操作数可以为数字的命令
		ArrayList<String> numberAvailable= new ArrayList<String>(){{
			add("add"); add("sub"); add("mul"); add("rem");
		}};

		// 左操作数必须获取寄存器
		Value left = instruction.getOperands().get(0);
		String reg0 = getRegister(instruction);
		String reg1 = getRegister(left);

		Value right = instruction.getOperands().get(1);
		if (right instanceof ConstInt && numberAvailable.contains(op)) {
			// 右操作数为常量，无需获取寄存器
			int val = ((ConstInt) right).getValue();
			writer.write(String.format(op + " %s, %s, %d", reg0, reg1, val));
			writer.newLine();
		} else {
			// 其他情况，需要获取寄存器
			String reg2 = getRegister(right);
			writer.write(String.format(op + " %s, %s, %s", reg0, reg1, reg2));
			writer.newLine();
		}
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
		// 保存现场在被调用函数中进行

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
		// $a0存入栈中
		int primary = sp;
		sp -= 4;
		int a0Addr = sp;
		allocStack(primary - sp);
		saveToStack("$a0", a0Addr - sp);

		// 传递参数
		Value operand = putint.getOperands().get(0);
		String reg = getRegister(operand);
		writer.write(String.format("move $a0, %s", reg));
		writer.newLine();

		// 调用宏
		writer.write("putint");
		writer.newLine();

		// 恢复$a0
		loadFromStack("$a0", a0Addr - sp);
		freeStack(primary - sp);
		sp = primary;
	}

	private void generateMipsFromPutstr(Putstr putstr) throws IOException {
		// $a0、$v0存入栈中
		int primary = sp;
		sp -= 4;
		int a0Addr = sp;
		allocStack(primary - sp);
		saveToStack("$a0", a0Addr - sp);

		// 传递参数
		Value operand = putstr.getOperands().get(0);
		writer.write(String.format("la $a0, %s", operand.getAddr()));
		writer.newLine();

		// 调用宏
		writer.write("putstr");
		writer.newLine();

		// 恢复$a0、$v0
		loadFromStack("$a0", a0Addr - sp);
		freeStack(primary - sp);
		sp = primary;
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
		for (String reg : registerAvailable) {
			loadFromStack(reg, currentFunction.getRegisterAddr().get(reg) - sp);
		}
		// 释放栈空间
		freeStack(currentFunction.getPrimarySp() - sp);

		// 返回
		writer.write("jr $ra");
		writer.newLine();
	}

	private void generateMipsFromBr(Br b) throws IOException {
		ArrayList<Value> operands = b.getOperands();
		if (operands.size() == 1) {
			BasicBlock block = (BasicBlock) operands.get(0);
			writer.write("j " + block.getMipsLabel(currentFunction.getRawName()));
			writer.newLine();
		} else {
			Value cond = operands.get(0);
			BasicBlock trueBlock = (BasicBlock) operands.get(1);
			BasicBlock falseBlock = (BasicBlock) operands.get(2);

			String reg = getRegister(cond);
			writer.write(String.format("beqz %s, %s", reg, falseBlock.getMipsLabel(currentFunction.getRawName())));
			writer.newLine();
			writer.write(String.format("j %s", trueBlock.getMipsLabel(currentFunction.getRawName())));
			writer.newLine();
		}
	}

	private void generateMipsFromMove(Move move) throws IOException {
		Value left = move.getOperands().get(0);
		Value right = move.getOperands().get(1);
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
		String reg = registers.get(value);
		if (value instanceof ConstInt) {
			// 如果是数值字面量，需要先li
			writer.write(String.format("li %s, %d", reg, ((ConstInt) value).getValue()));
			writer.newLine();
		}
		return reg;
	}
}
