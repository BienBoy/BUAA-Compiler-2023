package Mips;

import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import Optimizer.Mips.MipsOptimizer;
import Optimizer.Mips.ReplacePhi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MipsGeneratorAfterMem2reg {
	private final IrModule module;
	private final BufferedWriter writer;
	private Value[] registerUsed = new Value[4];
	private LinkedHashMap<Value, Integer> registerUseMap = new LinkedHashMap<>();
	// 栈指针虚拟位置，初始时为0，仅用于计算偏移
	private int sp = 0;
	private Function currentFunction;

	public MipsGeneratorAfterMem2reg(IrModule module, BufferedWriter writer) {
		this.module = module;
		this.writer = writer;
	}

	public void generate() throws IOException {
		// 进行优化，消除phi指令
		new ReplacePhi().optimize(module);
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

		// 保存$ra等
		sp -= 4;
		function.setRaAddr(sp);
		allocStack(4);
		saveToStack("$ra", 0);

		// 为所有的中间变量分配空间
		int primary = sp;
		for (BasicBlock b : currentFunction.getBasicBlocks()) {
			for (Value v : b.getValues()) {
				if (v instanceof Alloca) {
					sp -= ((Alloca) v).getSize();
					v.setAddr("" + sp);
				} else if (!(v instanceof Br ||
						v instanceof Putint ||
						v instanceof Putstr ||
						v instanceof Ret ||
						v instanceof Store ||
						v instanceof Move
				)){
					sp -= 4;
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

		// 翻译完一个函数后，将registerUsed、registerUseMap清空
		registerUsed = new Value[4];
		registerUseMap = new LinkedHashMap<>();
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
				// 退出基本块时，释放$a0-$a3
				generateMipsFromBr((Br) instruction);
			} else if (instruction instanceof Move) {
				generateMipsFromMove((Move) instruction);
			}
		}
	}

	private void generateMipsFromStore(Store store) throws IOException {
		// 获取操作数的寄存器
		Value value = store.getOperands().get(0);
		int reg1 = getRegister(value);

		Value address = store.getOperands().get(1);
		if (address instanceof GlobalVariable) {
			String label = address.getRawName();
			writer.write(String.format("sw $a%d, %s", reg1, label));
			writer.newLine();
		} else if (address instanceof Getelementptr) {
			// 地址由getelementptr计算出
			int reg2 = getRegister(address);
			writer.write(String.format("sw $a%d, ($a%d)", reg1, reg2));
			writer.newLine();
		} else if (address instanceof Alloca) {
			// 必然是普通变量，地址为数值形式
			int addr = Integer.parseInt(address.getAddr());
			int offset = addr - sp;
			writer.write(String.format("sw $a%d, %d($sp)", reg1, offset));
			writer.newLine();
		}
	}

	private void generateMipsFromLoad(Load load) throws IOException {
		Value address = load.getOperands().get(0);
		if (address instanceof GlobalVariable) {
			String label = address.getRawName();
			// 为结果分配寄存器
			int reg1 = allocRegister(load);
			writer.write(String.format("lw $a%d, %s", reg1, label));
			writer.newLine();
		} else if (address instanceof Getelementptr) {
			// 地址由getelementptr计算出
			int reg2 = getRegister(address);
			// 为结果分配寄存器
			int reg1 = allocRegister(load);
			writer.write(String.format("lw $a%d, ($a%d)", reg1, reg2));
			writer.newLine();
		} else if (address instanceof Instruction) {
			int addr = Integer.parseInt(address.getAddr());
			int offset = addr - sp;
			// 为结果分配寄存器
			int reg1 = allocRegister(load);
			writer.write(String.format("lw $a%d, %d($sp)", reg1, offset));
			writer.newLine();
		}
	}
	private void generateMipsFromGetelementptr(Getelementptr g) throws IOException {
		ArrayList<Value> operands = g.getOperands();
		Value address = operands.get(0);
		if (address instanceof GlobalVariable) {
			int reg0 = allocRegister(g);
			// 全局数组，初始地址为label
			writer.write(String.format("la $a%d, %s", reg0, address.getRawName()));
			writer.newLine();
		} else if (address instanceof Alloca) {
			// 局部数组或指针，起始地址为$sp+offset
			int addr = Integer.parseInt(address.getAddr());
			int reg0 = allocRegister(g);
			writer.write(String.format("add $a%d, $sp, %d", reg0, addr - sp));
			writer.newLine();
		} else {
			// getelementptr
			int reg1 = getRegister(address);
			int reg0 = allocRegister(g);
			writer.write(String.format("move $a%d, $a%d", reg0, reg1));
			writer.newLine();
		}

		for (int i = 1; i < operands.size(); i++) {
			int layerSize = g.getSize(i - 1);
			if (operands.get(i) instanceof ConstInt) {
				int offset = layerSize * ((ConstInt) operands.get(i)).getValue();
				int reg0 = getRegister(g);
				writer.write(String.format("add $a%d, $a%d, %d", reg0, reg0, offset));
				writer.newLine();
			} else {
				int reg1 = getRegister(operands.get(i));
				int reg2 = allocRegister(new Value(""));
				int reg0 = getRegister(g);

				writer.write(String.format("mul $a%d, $a%d, %d", reg2, reg1, layerSize));
				writer.newLine();
				writer.write(String.format("add $a%d, $a%d, $a%d", reg0, reg0, reg2));
				writer.newLine();
			}
		}
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
		int reg1 = getRegister(left);

		Value right = instruction.getOperands().get(1);
		if (right instanceof ConstInt && numberAvailable.contains(op)) {
			// 右操作数为常量，无需获取寄存器
			int val = ((ConstInt) right).getValue();
			int reg0 = allocRegister(instruction);
			writer.write(String.format(op + " $a%d, $a%d, %d", reg0, reg1, val));
			writer.newLine();
		} else {
			// 其他情况，需要获取寄存器
			int reg2 = getRegister(right);
			int reg0 = allocRegister(instruction);
			writer.write(String.format(op + " $a%d, $a%d, $a%d", reg0, reg1, reg2));
			writer.newLine();
		}
	}

	private void generateMipsFromZext(Zext zext) throws IOException {
		// 由于只涉及i1到i32的转换，实际无需进行，
		// 但涉及寄存器的分配，需要进行一次move
		Value operand = zext.getOperands().get(0);
		int reg1 = getRegister(operand);
		int reg0 = allocRegister(zext);

		writer.write(String.format("move $a%d, $a%d", reg0, reg1));
		writer.newLine();
	}

	private void generateMipsFromCall(Call call) throws IOException {
		// 函数调用
		ArrayList<Value> operands = call.getOperands();
		Function function = (Function) operands.get(0);
		int primary = sp;

		// 参数压栈
		ArrayList<Integer> paramAddr = new ArrayList<>();
		for (int i = 1; i < operands.size(); i++) {
			// 参数分配栈空间
			sp -= 4; // 参数大小均为4
			paramAddr.add(sp);
		}

		// 生成分配栈空间的指令
		allocStack(primary - sp);

		for (int i = 1; i < operands.size(); i++) {
			Value value = operands.get(i);
			int addr = paramAddr.get(i - 1);
			// 获取参数值所在寄存器
			int reg = getRegister(value);
			// 存入栈
			saveToStack("$a" + reg, addr - sp);
		}
		// 保存现场 $a0-$a3
		freeAllReg();
		// 跳转
		writer.write(String.format("jal %s", function.getRawName()));
		writer.newLine();

		// 生成释放栈空间的指令
		freeStack(primary - sp);
		sp = primary;

		// 有返回值时，保存返回值至寄存器
		if (((SymbolTable.Function)function.getSymbol()).hasReturn()) {
			int reg = allocRegister(call);
			writer.write("move $a" + reg + ", $v0");
			writer.newLine();
		}
	}

	private void generateMipsFromGetInt(GetInt getInt) throws IOException {
		// 无需保存寄存器
		// 调用宏
		writer.write("getint");
		writer.newLine();

		// 保存$v0的值至寄存器
		int reg = allocRegister(getInt);
		writer.write(String.format("move $a%d, $v0", reg));
		writer.newLine();
	}

	private void generateMipsFromPutint(Putint putint) throws IOException {
		// 释放$a0
		if (registerUsed[0] != null) {
			freeRegister(registerUsed[0]);
		}

		// 传递参数
		Value operand = putint.getOperands().get(0);
		int reg = getRegister(operand);
		if (reg != 0) {
			writer.write(String.format("move $a0, $a%d", reg));
			writer.newLine();
		}

		// 调用宏
		writer.write("putint");
		writer.newLine();
	}

	private void generateMipsFromPutstr(Putstr putstr) throws IOException {
		// 释放$a0
		if (registerUsed[0] != null) {
			freeRegister(registerUsed[0]);
		}

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
				int reg = getRegister(value);
				writer.write(String.format("move $v0, $a%d", reg));
				writer.newLine();
			}
		}

		// 恢复$ra等
		loadFromStack("$ra", currentFunction.getRaAddr() - sp);

		// 释放栈空间
		freeStack(currentFunction.getPrimarySp() - sp);

		// 函数返回时，需释放$a0-$a3，但不需存回栈空间
		freeAllRegWithoutStore();

		// 返回
		writer.write("jr $ra");
		writer.newLine();
	}

	private void generateMipsFromBr(Br b) throws IOException {
		ArrayList<Value> operands = b.getOperands();
		if (operands.size() == 1) {
			BasicBlock block = (BasicBlock) operands.get(0);
			// 离开基本块前，释放寄存器
			freeAllReg();
			writer.write("j " + block.getMipsLabel(currentFunction.getRawName()));
			writer.newLine();
		} else {
			Value cond = operands.get(0);
			BasicBlock trueBlock = (BasicBlock) operands.get(1);
			BasicBlock falseBlock = (BasicBlock) operands.get(2);

			int reg = getRegister(cond);
			// 离开基本块前，释放寄存器
			freeAllReg();
			writer.write(String.format("beqz $a%d, %s", reg, falseBlock.getMipsLabel(currentFunction.getRawName())));
			writer.newLine();
			writer.write(String.format("j %s", trueBlock.getMipsLabel(currentFunction.getRawName())));
			writer.newLine();
		}
	}

	private void generateMipsFromMove(Move move) throws IOException {
		Value left = move.getOperands().get(0);
		Value right = move.getOperands().get(1);
		int reg0 = getRegister(left);
		int reg1 = getRegister(right);
		writer.write(String.format("move $a%d, $a%d", reg0, reg1));
		writer.newLine();
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

	private int allocRegister(Value value) throws IOException {
		for (int i = 0; i < 4; i++) {
			if (registerUsed[i] == null) {
				// 记录分配情况
				registerUsed[i] = value;
				registerUseMap.put(value, i);
				return i;
			}
		}

		// 无空闲寄存器，需要释放
		// 释放未访问时间最长的
		Value v = registerUseMap.keySet().iterator().next();
		int reg = registerUseMap.get(v);
		freeRegister(v);

		// 记录分配情况
		registerUsed[reg] = value;
		registerUseMap.put(value, reg);

		return reg;
	}

	// 用于获取操作数（中间变量）所在的寄存器
	private int getRegister(Value value) throws IOException {
		if (registerUseMap.containsKey(value)) {
			// 获取后更新访问时间
			int reg = registerUseMap.get(value);
			registerUseMap.remove(value);
			registerUseMap.put(value, reg);
			return reg;
		}

		// 分配寄存器并读入
		int reg = allocRegister(value);
		if (value instanceof ConstInt) {
			// 对于常量，通过li赋值
			writer.write(String.format("li $a%d, %d", reg, ((ConstInt) value).getValue()));
			writer.newLine();
			return reg;
		} else if (value instanceof Undef) {
			return reg;
		}

		loadFromStack("$a" + reg, Integer.parseInt(value.getAddr()) - sp);
		return reg;
	}

	private void freeRegister(Value value) throws IOException {
		int reg = registerUseMap.get(value);
		if (value instanceof Instruction) {
			// 保存至栈
			saveToStack("$a" + reg, Integer.parseInt(value.getAddr()) - sp);
		}
		registerUsed[reg] = null;
		registerUseMap.remove(value);
	}

	// 释放$a0-$a3，将值存回栈空间
	private void freeAllReg() throws IOException {
		for (Value value : registerUseMap.keySet()) {
			int reg = registerUseMap.get(value);
			if (registerUsed[reg] instanceof Instruction){
				// 保存至栈
				saveToStack("$a" + reg, Integer.parseInt(registerUsed[reg].getAddr()) - sp);
			}
			registerUsed[reg] = null;
		}
		registerUseMap.clear();
	}

	// 释放$a0-$a3，值不存回栈空间
	private void freeAllRegWithoutStore() {
		registerUsed = new Value[4];
		registerUseMap.clear();
	}
}
