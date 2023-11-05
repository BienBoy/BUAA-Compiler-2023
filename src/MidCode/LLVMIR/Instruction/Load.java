package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Instruction.Instruction;
import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Load extends Instruction {
	public Load(String name, Value... operands) {
		super(name, operands);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		String operand = operands.get(0).getName();
		String type = operands.get(0).getType();
		writer.write("\t" + name + " = load " + getType() + ", " + type + " " + operand);
		writer.newLine();
	}

	@Override
	public String getType() {
		String type = operands.get(0).getType();
		return type.substring(0, type.length() - 1);
	}
}
