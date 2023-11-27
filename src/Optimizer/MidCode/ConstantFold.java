package Optimizer.MidCode;

import MidCode.IrRenamer;
import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import Optimizer.BaseOptimizer;

import java.util.HashMap;
import java.util.Map;

/**
 * 简单的常量折叠，将可以计算出值的表达式算出
 */
public class ConstantFold extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		for (Function function : module.getFunctions()) {
			for (BasicBlock basicBlock : function.getBasicBlocks()) {
				constantFold(basicBlock);
			}
		}
		new IrRenamer(module).rename();
	}

	private void constantFold(BasicBlock basicBlock) {
		boolean change = true;
		while (change) {
			change = false;
			Map<Instruction, Value> toReplace = new HashMap<>();
			for (Instruction instruction : basicBlock.getInstructions()) {
				if (instruction instanceof Zext) {
					// SysY中使用Zext仅涉及i1到i32的转换
					Value operand = instruction.getOperands().get(0);
					if (operand instanceof ConstInt) {
						change = true;
						toReplace.put(instruction, operand);
					}
					continue;
				}

				if (!isBinary(instruction)) {
					continue;
				}

				Value left = instruction.getOperands().get(0);
				Value right = instruction.getOperands().get(1);
				if (left instanceof ConstInt && right instanceof ConstInt) {
					change = true;
					int result = calculate(instruction);
					toReplace.put(instruction, new ConstInt(result));
				} else if (left instanceof ConstInt) {
					if (instruction.isCommutative()) {
						change = true;
						instruction.getOperands().set(0, right);
						instruction.getOperands().set(1, left);
					}
				} else if (right instanceof ConstInt) {
					if (((ConstInt) right).getValue() == 0) {
						if (instruction instanceof Add || instruction instanceof Sub) {
							change = true;
							toReplace.put(instruction, left);
						} else if (instruction instanceof Mul) {
							change = true;
							toReplace.put(instruction, new ConstInt(0));
						}
					} else if (((ConstInt) right).getValue() == 1) {
						if (instruction instanceof Mul || instruction instanceof Sdiv) {
							change = true;
							toReplace.put(instruction, left);
						} else if (instruction instanceof Srem) {
							change = true;
							toReplace.put(instruction, new ConstInt(0));
						}
					}
				}
			}

			for (Instruction ins : toReplace.keySet()) {
				ins.replaceUsed(toReplace.get(ins));
				ins.delete();
			}
		}
	}

	private boolean isBinary(Instruction instruction) {
		return instruction instanceof Add || instruction instanceof Sub ||
				instruction instanceof Mul || instruction instanceof Sdiv ||
				instruction instanceof Srem || instruction instanceof Icmp;
	}

	private int calculate(Instruction instruction) {
		ConstInt left = (ConstInt) instruction.getOperands().get(0);
		ConstInt right = (ConstInt) instruction.getOperands().get(1);

		if (instruction instanceof Add) {
			return left.getValue() + right.getValue();
		} else if (instruction instanceof Sub) {
			return left.getValue() - right.getValue();
		} else if (instruction instanceof Mul) {
			return left.getValue() * right.getValue();
		} else if (instruction instanceof Sdiv) {
			return left.getValue() / right.getValue();
		} else if (instruction instanceof Srem) {
			return left.getValue() % right.getValue();
		} else if (instruction instanceof Gt) {
			return left.getValue() > right.getValue() ? 1 : 0;
		} else if (instruction instanceof Ge) {
			return left.getValue() >= right.getValue() ? 1 : 0;
		} else if (instruction instanceof Lt) {
			return left.getValue() < right.getValue() ? 1 : 0;
		} else if (instruction instanceof Le) {
			return left.getValue() <= right.getValue() ? 1 : 0;
		} else if (instruction instanceof Eq) {
			return left.getValue() == right.getValue() ? 1 : 0;
		} else if (instruction instanceof Ne) {
			return left.getValue() != right.getValue() ? 1 : 0;
		}
		throw new RuntimeException();
	}
}
