package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Sub extends Instruction {
	public Sub(String name, Value... operands) {
		super(name, operands);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		String left = operands.get(0).getName();
		String right = operands.get(1).getName();
		writer.write("\t" + name + " = sub i32 " + left + ", " + right);
		writer.newLine();
	}
}
