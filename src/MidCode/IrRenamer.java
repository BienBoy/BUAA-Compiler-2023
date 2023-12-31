package MidCode;

import MidCode.LLVMIR.BasicBlock;
import MidCode.LLVMIR.Function;
import MidCode.LLVMIR.Instruction.Instruction;
import MidCode.LLVMIR.IrModule;

public class IrRenamer {
	private int counter;
	private final IrModule module;

	public IrRenamer(IrModule module) {
		this.module = module;
	}

	public void rename() {
		for (Function function : module.getFunctions()) {
			renameFunction(function);
		}
	}

	private void renameFunction(Function function) {
		counter = function.getParams().size();
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			renameBasicBlock(basicBlock);
		}
	}

	private void renameBasicBlock(BasicBlock basicBlock) {
		basicBlock.setName(String.valueOf(counter));
		counter++;

		for (Instruction instruction : basicBlock.getInstructions()) {
			if (instruction.hasResult()) {
				instruction.setName("%" + counter);
				counter++;
			}
		}
	}
}
