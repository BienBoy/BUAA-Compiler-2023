package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public abstract class Icmp extends Instruction {
	private static String op = "";
	public Icmp(String name, Value... operands) {
		super(name, operands);
	}

	public abstract String getOp();

	@Override
	public void output(BufferedWriter writer) throws IOException {
		String left = operands.get(0).getName();
		String right = operands.get(1).getName();
		writer.write("\t" + name + " = icmp " + getOp() + " i32 " + left + ", " + right);
		writer.newLine();
	}
}
