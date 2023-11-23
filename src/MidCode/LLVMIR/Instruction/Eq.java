package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Eq extends Icmp {
	public Eq(String name, Value... operands) {
		super(name, operands);
	}

	@Override
	public String getOp() {
		return "eq";
	}

	@Override
	public boolean isCommutative() {
		return true;
	}
}
