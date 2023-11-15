package MidCode.LLVMIR;


import SymbolTable.Array1D;
import SymbolTable.Array2D;
import SymbolTable.Symbol;
import SymbolTable.Variable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Value {
	protected final ArrayList<Use> useList; // 记录被使用情况
	protected String name; // Value的名字
	protected Symbol symbol; // Value对应的符号表项
	protected String addr;

	public Value() {
		useList = new ArrayList<>();
	}

	public Value(String name) {
		this.name = name;
		useList = new ArrayList<>();
	}

	public Value(Symbol symbol) {
		this.symbol = symbol;
		useList = new ArrayList<>();
	}

	public Value(String name, Symbol symbol) {
		this.name = name;
		this.symbol = symbol;
		useList = new ArrayList<>();
	}

	public void addUse(Use use) {
		useList.add(use);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<Use> getUseList() {
		return useList;
	}

	public Symbol getSymbol() {
		return symbol;
	}

	public void setSymbol(Symbol symbol) {
		this.symbol = symbol;
	}

	public String getType() {
		return "";
	}

	public void output(BufferedWriter writer) throws IOException {
		return;
	}

	public String getRawName() {
		return name.substring(1);
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	/**
	 * 替换所有使用本value的位置为新的value
	 * @param value 新的value
	 */
	public void replaceUsed(Value value) {
		for (Use use : useList) {
			use.getUser().replaceUse(this, value);
		}
	}
}
