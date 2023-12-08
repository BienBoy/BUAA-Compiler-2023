package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Ret extends Instruction {

	public Ret() {}
	public Ret(Value... operands) {
		super(operands);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		if (operands.isEmpty()) {
			writer.write("\t" + "ret void");
		} else {
			String operand = operands.get(0).getName();
			writer.write("\t" + "ret i32 " + operand);
		}
		writer.newLine();
	}
}
