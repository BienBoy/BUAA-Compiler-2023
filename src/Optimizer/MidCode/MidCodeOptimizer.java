package Optimizer.MidCode;

import MidCode.LLVMIR.IrModule;
import Optimizer.BaseOptimizer;

public class MidCodeOptimizer extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		// 必须先进行一次死代码删除，删除不可达的基本块，这是为了确保CFG构建不出错
		new DeadCode().optimize(module);
		new Mem2reg().optimize(module);
		new DeadCode().optimize(module);
		new Inline().optimize(module);
		new DeadCode().optimize(module);
		new GVN().optimize(module);
		new GCM().optimize(module);
		new ConstantFold().optimize(module);
	}
}
