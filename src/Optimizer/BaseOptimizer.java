package Optimizer;

import MidCode.LLVMIR.IrModule;

public abstract class BaseOptimizer {
	public abstract void optimize(IrModule module);
}
