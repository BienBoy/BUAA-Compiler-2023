package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Lt extends Icmp {
	public Lt(String name, Value... operands) {
		super(name, operands);
	}

	@Override
	public String getOp() {
		return "slt";
	}
}
