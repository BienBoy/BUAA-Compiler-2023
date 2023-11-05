package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.User;
import MidCode.LLVMIR.Value;

/**
 * 指令类基类
 */
public class Instruction extends User {
	public Instruction(Value...operands) {
		super(operands);
	}

	public Instruction(String name, Value...operands) {
		super(name, operands);
	}

	@Override
	public String getType() {
		return "i32";
	}
}
