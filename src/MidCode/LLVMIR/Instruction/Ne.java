package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;

public class Ne extends Icmp {
	public Ne(String name, Value... operands) {
		super(name, operands);
	}

	@Override
	public String getOp() {
		return "ne";
	}

	@Override
	public boolean isCommutative() {
		return true;
	}
}
