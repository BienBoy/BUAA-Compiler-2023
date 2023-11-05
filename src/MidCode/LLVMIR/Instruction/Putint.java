package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Putint extends Instruction {
	public Putint(Value... operands) {
		super(operands);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		String operand = operands.get(0).getName();
		writer.write("\tcall void @putint(i32 " + operand + ")");
		writer.newLine();
	}
}
