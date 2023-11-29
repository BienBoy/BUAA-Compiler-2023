package Optimizer.Mips;

import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import Optimizer.BaseOptimizer;
import Optimizer.CFG;
import SymbolTable.Variable;

import java.util.*;

public class RegisterAlloc extends BaseOptimizer {
	private final Set<String> registerAvailable = new HashSet<String>(){{
		add("$t0");add("$t1");add("$t2");add("$t3");add("$t4");add("$t5");
		add("$t6");add("$t7");add("$t8");add("$t9");add("$s0");add("$s1");
		add("$s2");add("$s3");add("$s4");add("$s5");add("$s6");add("$s7");
	}}; // 可用寄存器
	private final int registerNum = registerAvailable.size(); // 可分配寄存器的数量
	private final String[] constIntRegisters = new String[]{"$a0", "$a1"};
	private CFG graph;
	private Map<BasicBlock, Set<Value>> ins;
	private Map<BasicBlock, Set<Value>> outs;
	private Map<BasicBlock, Set<Value>> defs;
	private Map<BasicBlock, Set<Value>> uses;
	private UndirectedGraph conflictGraph;
	private UndirectedGraph conflictGraphBackup;
	private UndirectedGraph moveRelated; // 记录move相关结点
	private List<Value> stack; // 暂存需着色结点
	private Map<Value, Value> coalesces; // 记录合并的结点
	private Map<Value, String> functionRegisters;
	private Map<Value, String> registers = new HashMap<>();

	@Override
	public void optimize(IrModule module) {
		for (Function function : module.getFunctions()) {
			allocRegisters(function);
		}
	}

	public Map<Value, String> getRegisters() {
		return registers;
	}

	private boolean doSimplify, doCoalesce, doFreeze, restart;
	public void allocRegisters(Function function) {
		restart = true;
		while (restart) {
			restart = false;
			graph = new CFG(function);
			ins = new LinkedHashMap<>();
			outs = new LinkedHashMap<>();
			defs = new LinkedHashMap<>();
			uses = new LinkedHashMap<>();
			conflictGraph = new UndirectedGraph();
			stack = new ArrayList<>();
			coalesces = new HashMap<>();
			functionRegisters =  new HashMap<>();
			moveRelated = new UndirectedGraph();

			// 构建
			build(function);

			// 备份冲突图
			try {
				conflictGraphBackup = (UndirectedGraph) conflictGraph.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(e);
			}

			while (!conflictGraph.isEmpty()) {
				doFreeze = true;
				while (doFreeze) {
					doFreeze = false;
					doSimplify = doCoalesce = true;
					while (doSimplify || doCoalesce) {
						doSimplify = doCoalesce = false;
						// 简化
						simplify();
						// 合并，有bug，暂不进行
						// coalesce();
					}
					// 冻结
					if (!conflictGraph.isEmpty()) {
						freeze();
					}
				}
				// 溢出
				if (!conflictGraph.isEmpty()) {
					spill();
				}
			}
			// 选择
			select(function);
		}
		// 记录分配情况
		registers.putAll(functionRegisters);
	}

	private void build(Function function) {
		// 获取每个块的def和use，并初始化in和out
		buildDefsAndUses(function);
		// 迭代求解in和out
		buildInsAndOuts(function);
		// 构建冲突图
		buildConflictGraph(function);
		// 分类传送无关结点
		buildMoveRelated(function);
	}

	private void buildDefsAndUses(Function function) {
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			Set<Value> def = new HashSet<>();
			Set<Value> use = new HashSet<>();
			for (Instruction instruction : basicBlock.getInstructions()) {
				// Empty、Move与其他指令不同，需要特殊处理
				if (instruction instanceof Alloca || instruction instanceof Empty) {
					continue;
				}

				if (instruction instanceof Move) {
					Move move = (Move) instruction;
					Value target = move.getOperands().get(0);
					Value source = move.getOperands().get(1);
					if (source instanceof ConstInt || source instanceof Undef) {
						functionRegisters.put(source, allocConstIntRegister());
					} else if (!def.contains(source)) {
						// 操作数添加至use
						use.add(source);
					}
					if (!use.contains(target)) {
						def.add(target);
					}
					continue;
				}

				for (Value operand : instruction.getOperands()) {
					if (operand instanceof Alloca || operand instanceof GlobalVariable ||
							operand instanceof BasicBlock || operand instanceof ConstString ||
							operand instanceof FunctionParam) {
						// 内存地址(Alloca、GlobalVariables、字符串)、标签不参与寄存器分配
						continue;
					}
					if (operand instanceof ConstInt || operand instanceof Undef) {
						// 对于数值常量、undef，直接交替分配寄存器
						functionRegisters.put(operand, allocConstIntRegister());
						continue;
					}

					if (!def.contains(operand)) {
						// 操作数添加至use
						use.add(operand);
					}
				}
				if (!use.contains(instruction) && instruction.hasResult()) {
					def.add(instruction);
				}
			}
			defs.put(basicBlock, def);
			uses.put(basicBlock, use);
			ins.put(basicBlock, new HashSet<>());
			outs.put(basicBlock, new HashSet<>());
		}
	}

	private void buildInsAndOuts(Function function) {
		boolean change = true;
		while (change) {
			change = false;
			ArrayList<BasicBlock> basicBlocks = function.getBasicBlocks();
			for (int i = basicBlocks.size() - 1; i >= 0; i--) {
				BasicBlock basicBlock = basicBlocks.get(i);
				Set<Value> use = uses.get(basicBlock);
				Set<Value> def = defs.get(basicBlock);
				Set<Value> in = ins.get(basicBlock);
				Set<Value> out = outs.get(basicBlock);
				int inSize = in.size(), outSize = out.size();
				// out = 所有后继块的in的并集
				for (BasicBlock next : graph.getNextNodes().get(basicBlock)) {
					out.addAll(ins.get(next));
				}
				// in = use ∪ (out - def)
				Set<Value> temp = new HashSet<>(out);
				temp.removeAll(def); // out-def
				in.addAll(temp);
				in.addAll(use); // in中元素不会减少，因此可以这样写

				change = change || inSize != in.size() || outSize != out.size();
			}
		}
	}

	private void buildConflictGraph(Function function) {
		defs.values().forEach(e->e.forEach(conflictGraph::add));
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			ArrayList<Instruction> instructions = basicBlock.getInstructions();
			Set<Value> out = new HashSet<>(outs.get(basicBlock));
			for (int i = instructions.size() - 1; i >= 0; i--) {
				Instruction instruction = instructions.get(i);
				if (instruction instanceof Alloca || instruction instanceof Empty) {
					continue;
				}

				if (instruction instanceof Move) {
					Move move = (Move) instruction;
					Value target = move.getOperands().get(0);
					Value source = move.getOperands().get(1);
					out.forEach(e -> conflictGraph.add(target, e));
					out.remove(target);
					if (!(source instanceof ConstInt || source instanceof Undef)) {
						out.add(source);
					}
					continue;
				}

				// 变量定义处所有出口活跃的变量和定义的变量是互相冲突的
				if (instruction.hasResult()) {
					// 当前为变量定义语句
					out.forEach(e -> conflictGraph.add(instruction, e));
				}

				// 更新out
				out.remove(instruction);
				instruction.getOperands().forEach(e -> {
					if (e instanceof Instruction && !(e instanceof Alloca)) {
						out.add(e);
					}
				});
			}
		}
	}


	private void buildMoveRelated(Function function) {
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			for (Instruction instruction : basicBlock.getInstructions()) {
				if (instruction instanceof Move) {
					Value target = instruction.getOperands().get(0);
					Value source = instruction.getOperands().get(1);
					if (source instanceof ConstInt || source instanceof Undef) {
						// 常量或undef无法合并
						continue;
					}
					moveRelated.add(target, source);
				}
			}
		}
	}

	private void simplify() {
		boolean change = true;
		while (change) {
			change = false;
			Value toSimplify = null;
			for (Value value : conflictGraph.getAdjs().keySet()) {
				// 移除度小于registerNum的传送无关结点
				if (conflictGraph.getAdjs(value).size() < registerNum &&
						!moveRelated.contains(value)) {
					toSimplify = value;
					break;
				}
			}
			if (toSimplify != null) {
				change = doSimplify = true;
				stack.add(toSimplify);
				conflictGraph.remove(toSimplify);
			}
		}
	}

	private void coalesce() {
		// TODO 有bug
		// 保守合并无冲突的传送相关结点
		boolean coalesced = true;
		while (coalesced) {
			coalesced = false;
			Value first = null, second = null;
			for (Value a : moveRelated.getAdjs().keySet()) {
				for (Value b : moveRelated.getAdjs(a)) {
					// 根据Briggs条件判断能否合并
					if (!conflictGraph.hasEdge(a,b) && briggs(a, b)) {
						first = a;
						second = b;
						break;
					}
				}
				if (first != null && second != null) {
					break;
				}
			}
			if (first != null && second != null) {
				doCoalesce = coalesced = true;
				// 合并结点
				conflictGraph.coalesce(first, second);
				// 记录合并
				coalesces.put(second, first);
				// moveRelated中两结点合并
				moveRelated.coalesce(first, second);
			}
		}
	}

	private boolean briggs(Value a, Value b) {
		// 根据Briggs条件判断能否合并，即：
		// a、b合并后的结点ab的度数大于等于registerNum的邻结点数量小于registerNum
		UndirectedGraph copy;
		try {
			copy = (UndirectedGraph) conflictGraph.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		copy.coalesce(a, b);
		Set<Value> aAdjs = copy.getAdjs(a);
		int count = 0;
		for (Value aAdj : aAdjs) {
			if (copy.getAdjs(aAdj).size() >= registerNum) {
				count++;
			}
		}
		return count < registerNum;
	}

	private boolean george(Value a, Value b) {
		// 根据George条件判断能否合并，即：
		// a的所有高度数结点均和b冲突
		for (Value value : conflictGraph.getAdjs(a)) {
			if (conflictGraph.getAdjs(value).size() < registerNum) {
				continue;
			}
			if (!conflictGraph.hasEdge(value, b)) {
				return false;
			}
		}
		return true;
	}

	private void freeze() {
		// 寻找一个度数小于K的结点，此时该结点一定为传送结点，放弃合并，直接简化
		Value chosen = null;
		for (Value value : conflictGraph.getAdjs().keySet()) {
			if (conflictGraph.getAdjs(value).size() < registerNum) {
				chosen = value;
				break;
			}
		}
		if (chosen != null) {
			doFreeze = true;
			stack.add(chosen);
			conflictGraph.remove(chosen);

			// 冻结结点，即不再是传送无关结点
			// 从moveRelated中移除该结点
			moveRelated.remove(chosen);
		}
	}

	private void spill() {
		// 选择一个高度数结点作为潜在溢出结点
		Value chosen = conflictGraph.getAdjs().keySet().iterator().next();
		if (moveRelated.contains(chosen)) {
			// 从moveRelated中移除该结点
			moveRelated.remove(chosen);
		}
		stack.add(chosen);
		conflictGraph.remove(chosen);
	}

	private void select(Function function) {
		for (int i = stack.size() - 1; i >= 0; i--) {
			Value value = stack.get(i);
			Set<String> available = new HashSet<>(registerAvailable);
			conflictGraphBackup.getAdjs(value).forEach(e->{
				available.remove(functionRegisters.get(e));
			});
			if (available.isEmpty()) {
				// 实际溢出，需要更改该变量的使用为内存格式重新开始
				restart = true;
				changeToLoadStore(value, function);
				return;
			}
			String reg = available.iterator().next();
			functionRegisters.put(value, reg);
			// 合并的结点使用同一寄存器
			shareRegister(value, reg);
		}
	}

	private void changeToLoadStore(Value value, Function function) {
		assert value instanceof Instruction;
		Alloca alloca = new Alloca("%-1");
		alloca.setSymbol(new Variable("temp"));
		function.getBasicBlocks().get(0).add(0, alloca);
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			for (int i = 0; i < basicBlock.getInstructions().size(); i++) {
				Instruction instruction = basicBlock.getInstructions().get(i);
				if (instruction instanceof Alloca || instruction instanceof Empty) {
					continue;
				}

				if (instruction instanceof Move) {
					Move move = (Move) instruction;
					if (move.getOperands().get(1) == value) {
						// 使用前先load
						Load load = new Load("%-1", alloca);
						basicBlock.add(i, load);
						move.replaceUse(value, load);
						i++;
					}
					if (move.getOperands().get(0) == value) {
						// 替换为store
						basicBlock.replaceInstruction(move, new Store(move.getOperands().get(1), alloca));
					}
					continue;
				}

				if (instruction.getOperands().contains(value)) {
					// 使用前先load
					Load load = new Load("%-1", alloca);
					basicBlock.add(i, load);
					instruction.replaceUse(value, load);
					i++;
				}
				if (instruction == value) {
					// 为定义语句，后面添加一条store
					basicBlock.add(i + 1, new Store(instruction, alloca));
					i++;
				}
			}
		}
	}

	private void shareRegister(Value a, String reg) {
		for (Value b : coalesces.keySet()) {
			if (coalesces.get(b) == a) {
				functionRegisters.put(b, reg);
				shareRegister(b, reg);
			}
		}
	}

	private int pos = 1;
	private String allocConstIntRegister() {
		pos = 1 - pos;
		return constIntRegisters[pos];
	}
}
