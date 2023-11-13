package Optimizer;

import MidCode.LLVMIR.IrModule;

public class Optimizer extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		new DeadCode().optimize(module);
	}
}
