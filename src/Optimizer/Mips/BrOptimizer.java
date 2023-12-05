package Optimizer.Mips;

import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.Br;
import MidCode.LLVMIR.Instruction.Instruction;
import MidCode.LLVMIR.Instruction.Ret;
import Optimizer.BaseOptimizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BrOptimizer extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		for (Function function : module.getFunctions()) {
			brOptimize(function);
		}
	}

	private void brOptimize(Function function) {
		// 替换条件已知的条件跳转为无条件跳转
		replaceConditonBr(function);
		// 合并基本块
		mergeBasicBlock(function);
	}

	private void replaceConditonBr(Function function) {
		// 替换条件已知的条件跳转为无条件跳转
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			List<Instruction> instructions = basicBlock.getInstructions();
			Instruction last = instructions.get(instructions.size() - 1);
			if (last instanceof Ret) {
				continue;
			}
			if (last.getOperands().size() == 1 || !(last.getOperands().get(0) instanceof ConstInt)) {
				continue;
			}
			int cond = ((ConstInt) last.getOperands().get(0)).getValue();
			Br br;
			if (cond == 0) {
				br = new Br(last.getOperands().get(2));
			} else {
				br = new Br(last.getOperands().get(1));
			}
			basicBlock.replaceInstruction(last, br);
		}
	}

	private void mergeBasicBlock(Function function) {
		boolean change = true;
		while (change) {
			change = false;
			Set<BasicBlock> toRemove = new HashSet<>();
			for (BasicBlock basicBlock : function.getBasicBlocks()) {
				if (basicBlock.getInstructions().size() != 1){
					continue;
				}
				Instruction last = basicBlock.getInstructions().get(basicBlock.getInstructions().size() - 1);
				if (!(last instanceof Br && last.getOperands().size() == 1)) {
					continue;
				}

				List<Use> uses = new ArrayList<>(basicBlock.getUseList());
				for (Use use : uses) {
					change = true;
					toRemove.add(basicBlock);
					Instruction instruction = (Instruction) use.getUser();
					instruction.replaceUse(basicBlock, last.getOperands().get(0));
				}
			}

			for (BasicBlock basicBlock : toRemove) {
				basicBlock.getInstructions().forEach(User::removeUse);
				function.getBasicBlocks().remove(basicBlock);
			}
		}
	}
}
