package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * 空指令，非LLVM IR指令。
 * Empty仅用于引入一个新的变量，不会被翻译为mips
 */
public class Empty extends Instruction {
	public Empty() {
		super();
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		writer.write("\t" + name + " = " + name);
		writer.newLine();
	}

	@Override
	public boolean hasResult() {
		return true;
	}
}
