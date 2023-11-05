package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.ConstString;
import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Putstr extends Instruction {
	public Putstr(Value... operands) {
		super(operands);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		ConstString constString = (ConstString) operands.get(0);
		String address = constString.getName();
		String shape = constString.getShape();
		writer.write("\tcall void @putstr(i8* getelementptr (");
		writer.write(shape + ", " + shape + "* " + address);
		writer.write(", i32 0, i32 0))");
		writer.newLine();
	}
}
