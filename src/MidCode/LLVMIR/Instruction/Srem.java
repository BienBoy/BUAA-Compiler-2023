package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Srem extends Instruction {
	public Srem(String name, Value... operands) {
		super(name, operands);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		String left = operands.get(0).getName();
		String right = operands.get(1).getName();
		writer.write("\t" + name + " = srem i32 " + left + ", " + right);
		writer.newLine();
	}

	@Override
	public boolean hasResult() {
		return true;
	}
}
