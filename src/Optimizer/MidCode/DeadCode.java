package Optimizer.MidCode;

import MidCode.IrRenamer;
import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import Optimizer.BaseOptimizer;

import java.util.ArrayList;
import java.util.List;

public class DeadCode extends BaseOptimizer {
	private boolean change = true;
	@Override
	public void optimize(IrModule module) {
		while (change) {
			change = false;

			module.getGlobalVariables().removeIf(v-> {
				if (v.getUseList().isEmpty()) {
					change = true;
					return true;
				}
				return false;
			});

			module.getConstStrings().removeIf(s-> {
				if (s.getUseList().isEmpty()) {
					change = true;
					return true;
				}
				return false;
			});

			module.getFunctions().removeIf(f-> {
				if (f.getUseList().isEmpty() && !f.getRawName().equals("main")) {
					change = true;
					return true;
				}
				return false;
			});

			for (Function function : module.getFunctions()) {
				removeDeadInstructions(function);
			}
		}
		new IrRenamer(module).rename();
	}

	private void removeDeadInstructions(Function function) {
		// 删除不可达代码（未使用的块）
		function.getBasicBlocks().removeIf(b-> {
			if (!b.equals(function.getBasicBlocks().get(0)) &&
					b.getUseList().isEmpty()) {
				change = true;
				b.getInstructions().forEach(User::removeUse);
				return true;
			}
			return false;
		});

		// 删除每个块内未被使用的指令
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			removeDeadInstructions(basicBlock);
		}
	}

	private void removeDeadInstructions(BasicBlock basicBlock) {
		// 遍历删除计算结果未被使用的指令
		List<Instruction> toDelete = new ArrayList<>();
		for (Instruction instruction : basicBlock.getInstructions()) {
			if (canDeleteInstruction(instruction)) {
				change = true;
				toDelete.add(instruction);
			}
		}
		toDelete.forEach(Instruction::delete);
	}

	private boolean canDeleteInstruction(Instruction i) {
		return i.getUseList().isEmpty() && (i instanceof Add ||
				i instanceof Sub || i instanceof Mul || i instanceof Sdiv ||
				i instanceof Srem || i instanceof Icmp || i instanceof Zext ||
				i instanceof Load || i instanceof Alloca || i instanceof Phi);
	}
}
