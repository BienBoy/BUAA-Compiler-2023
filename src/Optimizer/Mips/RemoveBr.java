package Optimizer.Mips;

import MidCode.LLVMIR.BasicBlock;
import MidCode.LLVMIR.Function;
import MidCode.LLVMIR.Instruction.Br;
import MidCode.LLVMIR.Instruction.Instruction;
import MidCode.LLVMIR.IrModule;
import Optimizer.BaseOptimizer;

public class RemoveBr extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		for (Function function : module.getFunctions()) {
			for (int i = 0; i < function.getBasicBlocks().size(); i++) {
				BasicBlock basicBlock = function.getBasicBlocks().get(i);
				Instruction last = basicBlock.getInstructions().get(basicBlock.getInstructions().size() - 1);
				if (last instanceof Br && last.getOperands().size() == 1) {
					BasicBlock next = i + 1 < function.getBasicBlocks().size() ? function.getBasicBlocks().get(i + 1) : null;
					if (last.getOperands().get(0) == next) {
						basicBlock.getInstructions().remove(last);
					}
				}
			}
		}
	}
}
