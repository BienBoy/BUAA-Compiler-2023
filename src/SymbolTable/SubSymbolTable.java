package SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 子表，一个作用域有一个
 */
public class SubSymbolTable {
	private final SubSymbolTable outer; // 外层符号表
	private final ArrayList<SubSymbolTable> inner; // 内层符号表
	private Function function; // 子表位于哪个函数内；全局的为null
	private final HashMap<String, Symbol> symbols; // 单个符号表内的符号

	public SubSymbolTable(SubSymbolTable outer, Function function) {
		this.outer = outer;
		this.inner = new ArrayList<>();
		this.symbols = new HashMap<>();
		this.function = function;
	}

	/**
	 * 查找标识符
	 * @param name 标识符名字
	 * @return 标识符对应的Symbol，若无则返回null
	 */
	public Symbol search(String name) {
		SubSymbolTable table = this;
		Symbol symbol = symbols.get(name);

		if (symbol == null && outer != null)
			symbol = outer.search(name);

		return symbol;
	}

	/**
	 * 向符号表中添加符号
	 * @param symbol 要添加的符号
	 * @return 若本层符号表中已有符号，返回false；否则返回true
	 */
	public boolean insert(Symbol symbol) {
		if (symbols.get(symbol.getName()) != null)
			return false;

		symbols.put(symbol.getName(), symbol);
		return true;
	}

	/**
	 * 添加符号表至内层符号表
	 * @param table 要添加的符号表
	 */
	public void addInner(SubSymbolTable table) {
		this.inner.add(table);
	}

	public SubSymbolTable getOuter() {
		return outer;
	}

	public Function getFunction() {
		return function;
	}
}
