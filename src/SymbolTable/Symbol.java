package SymbolTable;

public abstract class Symbol {
	private final String name; // 标识符名字

	private int addr; // 地址

	public Symbol(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int getAddr() {
		return addr;
	}
}
