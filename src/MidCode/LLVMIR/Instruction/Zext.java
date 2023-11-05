package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Zext extends Instruction {
	private final String sorceType;
	private final String targetType;
	public Zext(String name, String sorceType, String targetType, Value... operands) {
		super(name, operands);
		this.sorceType = sorceType;
		this.targetType = targetType;
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		String operand = operands.get(0).getName();
		writer.write("\t" + name + " = zext " + sorceType + " " + operand + " to " + targetType);
		writer.newLine();
	}
}
