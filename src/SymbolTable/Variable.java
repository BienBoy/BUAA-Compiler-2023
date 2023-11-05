package SymbolTable;

public class Variable extends ConstSymbol {
	private Integer value; // 常量的值
	public Variable(String name) {
		super(name, false);
	}

	public Variable(String name, Integer value, boolean constant) {
		super(name, constant);
		this.value = value;
	}

	public Integer getValue() {
		if (value == null && isGlobal())
			return 0;
		return value;
	}

	public boolean isGlobal() {
		return this.getSubSymbolTable().getOuter() == null;
	}

	public boolean isGlobalConst() {
		return this.isConstant() && this.isGlobal();
	}
}
