package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

/**
 * 非LLVM IR指令，用于图着色法分配寄存器前的中间形式
 */
public class PushStack extends Instruction {
	public PushStack(Value... operands) {
		super(operands);
	}
}
