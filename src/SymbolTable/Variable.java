package SymbolTable;

public class Variable extends ConstSymbol {
	private int value; // 常量的值
	public Variable(String name) {
		super(name, false);
	}

	public Variable(String name, int value) {
		super(name, true);
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
