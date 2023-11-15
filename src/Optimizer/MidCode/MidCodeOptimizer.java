package Optimizer.MidCode;

import MidCode.LLVMIR.IrModule;
import Optimizer.BaseOptimizer;

public class MidCodeOptimizer extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		// 必选先进行一次死代码删除，删除不可达的基本块
		new DeadCode().optimize(module);
		new Mem2reg().optimize(module);
		new DeadCode().optimize(module);
	}
}
