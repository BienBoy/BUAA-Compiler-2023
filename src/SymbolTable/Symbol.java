package SymbolTable;

import MidCode.LLVMIR.Value;

public abstract class Symbol {
	private final String name; // 标识符名字
	private int addr; // 地址
	private String register; // 分配的寄存器
	private Value IRValue; // 对应的LLVM IR中的Value
	private SubSymbolTable subSymbolTable;

	public Symbol(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int getAddr() {
		return addr;
	}

	public void setAddr(int addr) {
		this.addr = addr;
	}

	public Value getIRValue() {
		return IRValue;
	}

	public void setIRValue(Value IRValue) {
		this.IRValue = IRValue;
	}

	public SubSymbolTable getSubSymbolTable() {
		return subSymbolTable;
	}

	public void setSubSymbolTable(SubSymbolTable subSymbolTable) {
		this.subSymbolTable = subSymbolTable;
	}

	public boolean isGlobal() {
		return subSymbolTable.isGlobal();
	}

	public String getRegister() {
		return register;
	}

	public void setRegister(String register) {
		this.register = register;
	}
}
