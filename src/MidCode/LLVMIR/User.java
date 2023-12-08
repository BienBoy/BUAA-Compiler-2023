package MidCode.LLVMIR;

import java.util.ArrayList;

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
			value.addUse(new Use(value, this));
			operands.add(value);
		}
	}

	public ArrayList<Value> getOperands() {
		return operands;
	}

	/**
	 * 移除本value对其他值的使用
	 */
	public void removeUse() {
		for (Value operand : operands) {
			operand.useList.removeIf(use -> use.getUser().equals(this));
		}
	}

	/**
	 * 将使用的Value由from替换为to
	 * @param from 要替换的Value
	 * @param to 替换后的Value
	 */
	public void replaceUse(Value from, Value to) {
		for (int i = 0; i < operands.size(); i++) {
			if (operands.get(i).equals(from)) {
				operands.set(i, to);
				// 维护useList
				to.addUse(new Use(to, this));
				to.useList.removeIf(use -> use.getUser().equals(from));
			}
		}
	}

	/**
	 * 将第一处使用的from替换为to
	 * @param from 要替换的Value
	 * @param to 替换后的Value
	 */
	public void replaceFirstUse(Value from, Value to) {
		for (int i = 0; i < operands.size(); i++) {
			if (operands.get(i).equals(from)) {
				operands.set(i, to);
				// 维护useList
				to.addUse(new Use(to, this));
				to.useList.removeIf(use -> use.getUser().equals(from));
				break;
			}
		}
	}
}
