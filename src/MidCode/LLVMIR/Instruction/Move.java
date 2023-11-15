package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * 并行复制指令，非LLVM IR指令。直接对应mips的伪指令move
 */
public class Move extends Instruction {
	public Move(Value... operands) {
		super(operands);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		writer.write("\tmove(" + operands.get(0).getName());
		writer.write(", " + operands.get(1).getName() + ")");
		writer.newLine();
	}
}
