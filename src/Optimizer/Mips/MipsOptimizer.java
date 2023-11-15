package Optimizer.Mips;

import MidCode.LLVMIR.IrModule;
import Optimizer.BaseOptimizer;

public class MipsOptimizer extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		new ReplacePhi().optimize(module);
	}
}
