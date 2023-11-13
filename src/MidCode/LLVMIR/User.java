package MidCode.LLVMIR;

import java.util.ArrayList;
import java.util.Arrays;

public class User extends Value {
	protected final ArrayList<Value> operands; // 使用的Value

	public User() {
		this.operands = new ArrayList<>();
	}

	public User(Value...operands) {
		this.operands = new ArrayList<>();
		addOperand(operands);
	}

	public User(String name, Value...operands) {
		super(name);
		this.operands = new ArrayList<>();
		addOperand(operands);
	}

	public void addOperand(Value...values) {
		for (Value value : values) {
			value.addUse(new Use(value, this, operands.size()));
			operands.add(value);
		}
	}

	public ArrayList<Value> getOperands() {
		return operands;
	}

	public void removeUse() {
		for (Value operand : operands) {
			operand.useList.removeIf(use -> use.getUser().equals(this));
		}
	}
}
