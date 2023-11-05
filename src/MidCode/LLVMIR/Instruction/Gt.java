package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Gt extends Icmp {
	public Gt(String name, Value... operands) {
		super(name, operands);
	}

	@Override
	public String getOp() {
		return "sgt";
	}
}
