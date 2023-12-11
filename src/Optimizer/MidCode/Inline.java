package Optimizer.MidCode;

import MidCode.IrRenamer;
import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import Optimizer.BaseOptimizer;

import java.util.*;

public class Inline extends BaseOptimizer {
	private Set<Function> canInline = new HashSet<>();
	@Override
	public void optimize(IrModule module) {
		// 先检查是否可以内联
		check(module);
		// 进行内联
		inline(module);
		new IrRenamer(module).rename();
	}

	private void check(IrModule module) {
		for (Function function : module.getFunctions()) {
			if (check(function)) {
				canInline.add(function);
			}
		}
	}

	private boolean check(Function function) {
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			for (Instruction instruction : basicBlock.getInstructions()) {
				if (instruction instanceof Call) {
					Function called = (Function) instruction.getOperands().get(0);
					if (called == function) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void inline(IrModule module) {
		for (Function function : module.getFunctions()) {
			if (canInline.contains(function)) {
				inline(module, function);
			}
		}
	}

	private void inline(IrModule module, Function inlined) {
		for (Function function : module.getFunctions()) {
			List<Instruction> toInline = new ArrayList<>();
			for (BasicBlock basicBlock : function.getBasicBlocks()) {
				for (Instruction instruction : basicBlock.getInstructions()) {
					if (instruction instanceof Call &&
							instruction.getOperands().get(0) == inlined) {
						toInline.add(instruction);
					}
				}
			}

			for (Instruction instruction : toInline) {
				// 将原本的基本块从函数调用语句处拆分为两个
				BasicBlock oldBlock = instruction.getBasicBlock();
				BasicBlock newBlock = new BasicBlock("-1");
				function.addBasicBlock(newBlock);
				boolean flag = false;
				for (Instruction ins : instruction.getBasicBlock().getInstructions()) {
					if (flag) {
						newBlock.add(ins);
					}
					if (ins == instruction) {
						flag = true;
					}
				}
				for (Instruction ins : newBlock.getInstructions()) {
					oldBlock.getInstructions().remove(ins);
				}
				for (BasicBlock b : function.getBasicBlocks()) {
					for (Instruction ins : b.getInstructions()) {
						// Phi指令中的oldBlock需替换为newBlock
						if (ins instanceof Phi) {
							for (int i = 0; i < ins.getOperands().size(); i += 2) {
								if (ins.getOperands().get(i) == oldBlock) {
									ins.getOperands().set(i, newBlock);
								}
							}
						}
					}
				}


				List<FunctionParam> params = inlined.getParams();
				Map<FunctionParam, Value> paramsMap = new HashMap<>();
				for (int i = 0; i < params.size(); i++) {
					paramsMap.put(params.get(i), instruction.getOperands().get(i + 1));
				}

				// 插入被内联的函数的基本块，替换函数形参为传入参数
				List<BasicBlock> inlinedBlock = copyFunctionBlock(inlined, paramsMap);
				List<Ret> rets = new ArrayList<>();

				for (Instruction ins : inlinedBlock.get(0).getInstructions()) {
					if (!(ins instanceof Alloca)) {
						break;
					}
					function.getBasicBlocks().get(0).add(0, ins);
				}
				inlinedBlock.get(0).getInstructions().removeIf(e -> e instanceof Alloca);

				for (BasicBlock basicBlock : inlinedBlock) {
					for (Instruction ins : basicBlock.getInstructions()) {
						if (ins instanceof Ret) {
							rets.add((Ret) ins);
						}
					}
					function.addBasicBlock(basicBlock);
				}

				if (instruction.hasResult()) {
					if (rets.size() == 1) {
						rets.get(0).getBasicBlock().replaceInstruction(rets.get(0), new Br(newBlock));
						instruction.replaceUsed(rets.get(0).getOperands().get(0));
					} else {
						List<Value> operands = new ArrayList<>();
						for (Ret ret : rets) {
							operands.add(ret.getBasicBlock());
							operands.add(ret.getOperands().get(0));
							ret.getBasicBlock().replaceInstruction(ret, new Br(newBlock));
						}
						Phi phi = new Phi("%-1", operands.toArray(new Value[0]));
						newBlock.add(0, phi);
						instruction.replaceUsed(phi);
					}
				} else {
					for (Ret ret : rets) {
						ret.getBasicBlock().replaceInstruction(ret, new Br(newBlock));
					}
				}
				// 函数调用替换为跳转
				oldBlock.replaceInstruction(instruction, new Br(inlinedBlock.get(0)));
			}
		}
	}

	private Map<BasicBlock, BasicBlock> basicBlockMap;
	private Map<Instruction, Instruction> instructionMap;
	private List<BasicBlock> copyFunctionBlock(Function function, Map<FunctionParam, Value> paramsMap) {
		List<BasicBlock> inlinedBlock = new ArrayList<>();
		basicBlockMap = new HashMap<>();
		instructionMap = new HashMap<>();
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			inlinedBlock.add(copyBasicBlock(basicBlock));
		}
		for (BasicBlock basicBlock : inlinedBlock) {
			for (Instruction instruction : basicBlock.getInstructions()) {
				replaceOperands(instruction, paramsMap);
			}
		}
		return inlinedBlock;
	}

	private BasicBlock copyBasicBlock(BasicBlock source) {
		BasicBlock copy = new BasicBlock("-1");
		basicBlockMap.put(source, copy);
		for (Instruction instruction : source.getInstructions()) {
			Instruction ins;
			try {
				if (instruction instanceof Zext) {
					Zext zext = (Zext) instruction;
					ins = new Zext(zext.getName(), zext.getSorceType(),
							zext.getTargetType(), zext.getOperands().get(0));
				} else if (instruction.hasResult() && !instruction.getOperands().isEmpty()) {
					ins = instruction.getClass()
							.getConstructor(String.class, Value[].class)
							.newInstance("%-1", instruction.getOperands().toArray(new Value[0]));
				} else if (instruction.hasResult()){
					ins = instruction.getClass()
							.getConstructor(String.class)
							.newInstance("%-1");
				} else if (!instruction.getOperands().isEmpty()) {
					ins = instruction.getClass()
							.getConstructor(Value[].class)
							.newInstance((Object) instruction.getOperands().toArray(new Value[0]));
				} else {
					ins = instruction.getClass()
							.getConstructor()
							.newInstance();
				}
				ins.setSymbol(instruction.getSymbol());
			} catch (Exception e) {
				throw new RuntimeException();
			}
			instructionMap.put(instruction, ins);
			copy.add(ins);
		}
		return copy;
	}

	private void replaceOperands(Instruction instruction, Map<FunctionParam, Value> paramsMap) {
		for (int i = 0; i < instruction.getOperands().size(); i++) {
			Value operand = instruction.getOperands().get(i);
			if (operand instanceof Instruction) {
				instruction.replaceFirstUse(operand, instructionMap.get(operand));
			} else if (operand instanceof FunctionParam) {
				instruction.replaceFirstUse(operand, paramsMap.get(operand));
			} else if (operand instanceof BasicBlock) {
				instruction.replaceFirstUse(operand, basicBlockMap.get(operand));
			}
		}
	}
}
