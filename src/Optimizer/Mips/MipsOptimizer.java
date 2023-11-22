package Optimizer.Mips;

import MidCode.LLVMIR.IrModule;
import MidCode.LLVMIR.Value;
import Optimizer.BaseOptimizer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class MipsOptimizer extends BaseOptimizer {
	private RegisterAlloc registerAlloc;
	@Override
	public void optimize(IrModule module) {
		new ReplacePhi().optimize(module);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("./replacephi.txt"));
			module.output(writer);
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		new Rewrite().optimize(module);
		registerAlloc = new RegisterAlloc();
		registerAlloc.optimize(module);
	}

	public Map<Value, String> getRegisters() {
		return registerAlloc.getRegisters();
	}
}
