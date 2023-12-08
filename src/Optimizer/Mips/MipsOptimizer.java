package Optimizer.Mips;

import MidCode.LLVMIR.Function;
import MidCode.LLVMIR.IrModule;
import MidCode.LLVMIR.Value;
import Optimizer.BaseOptimizer;
import Optimizer.MidCode.DeadCode;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class MipsOptimizer extends BaseOptimizer {
	private RegisterAlloc registerAlloc;
	@Override
	public void optimize(IrModule module) {
		new ReplacePhi().optimize(module);
		new BrOptimizer().optimize(module);
		new DeadCode().optimize(module); // 主要是为了删除未使用的块
		new Rewrite().optimize(module);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("./replacephi.txt"));
			module.output(writer);
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		registerAlloc = new RegisterAlloc();
		registerAlloc.optimize(module);
		new RemoveBr().optimize(module);
		new DeadCode().optimize(module); // 主要是为了删除空块
	}

	public Map<Value, String> getRegisters() {
		return registerAlloc.getRegisters();
	}

	public Map<Function, Set<Value>> getSpills() {
		return registerAlloc.getSpills();
	}
}
