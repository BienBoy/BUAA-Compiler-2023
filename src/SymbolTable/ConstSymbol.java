package SymbolTable;

public class ConstSymbol extends Symbol {
	private final boolean constant; // 是否为常量

	public ConstSymbol(String name, boolean constant) {
		super(name);
		this.constant = constant;
	}

	public boolean isConstant() {
		return constant;
	}
}
