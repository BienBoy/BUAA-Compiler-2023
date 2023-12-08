package Optimizer.Mips;

import MidCode.LLVMIR.IrModule;
import MidCode.LLVMIR.Value;
import Optimizer.BaseOptimizer;
import Optimizer.MidCode.DeadCode;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class MipsOptimizer extends BaseOptimizer {
	private RegisterAlloc registerAlloc;
	@Override
	public void optimize(IrModule module) {
		new ReplacePhi().optimize(module);
		new BrOptimizer().optimize(module);
		new DeadCode().optimize(module);
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
	}

	public Map<Value, String> getRegisters() {
		return registerAlloc.getRegisters();
	}
}
