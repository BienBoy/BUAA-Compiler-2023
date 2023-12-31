package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class GetInt extends Instruction {
	public GetInt(String name) {
		super(name);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		writer.write("\t" + name + " = call i32 @getint()");
		writer.newLine();
	}

	public boolean hasResult() {
		return true;
	}
}
