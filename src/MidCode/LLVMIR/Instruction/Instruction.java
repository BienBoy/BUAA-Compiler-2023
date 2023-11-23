package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.BasicBlock;
import MidCode.LLVMIR.User;
import MidCode.LLVMIR.Value;

/**
 * 指令类基类
 */
public abstract class Instruction extends User {
	private BasicBlock basicBlock;
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

	public BasicBlock getBasicBlock() {
		return basicBlock;
	}

	public void setBasicBlock(BasicBlock basicBlock) {
		this.basicBlock = basicBlock;
	}

	public void delete() {
		removeUse();
		basicBlock.getInstructions().remove(this);
	}

	public boolean hasResult() {
		return false;
	}

	public boolean isCommutative() {
		return false;
	}
}
