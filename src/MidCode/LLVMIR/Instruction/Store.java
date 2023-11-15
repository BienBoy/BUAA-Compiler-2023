package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.FunctionParam;
import MidCode.LLVMIR.GlobalVariable;
import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Store extends Instruction {
	public Store(Value... operands) {
		super(operands);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		String left = operands.get(0).getName();
		String ltype = operands.get(0).getType();
		String right = operands.get(1).getName();
		String rtype = operands.get(1).getType();
		writer.write("\tstore " + ltype + " " + left + ", " + rtype + " " + right);
		writer.newLine();
	}

	public Value getNewValue() {
		return operands.get(0);
	}

	public boolean isLocalVariable() {
		return !(operands.get(1) instanceof GlobalVariable) &&
				!(operands.get(1) instanceof Getelementptr);
	}
}
