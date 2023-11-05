package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Br extends Instruction {
	public Br(Value... operands) {
		super(operands);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		if (operands.size() == 1) {
			String label = operands.get(0).getName();
			writer.write("\tbr label %" + label);
			writer.newLine();
			return;
		}
		String cond = operands.get(0).getName();
		String label1 = operands.get(1).getName();
		String label2 = operands.get(2).getName();
		writer.write("\tbr i1 " + cond + ", label %" + label1 + ", label %" + label2);
		writer.newLine();
	}
}
