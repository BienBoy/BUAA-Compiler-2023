package SymbolTable;

import java.util.ArrayList;

/**
 * 符号表，包含若干子表。
 */
public class SymbolTable {
	private SubSymbolTable current; // 当前所在的子表

	/* 应位于内层作用域的符号，SysY语法中这类符号只有函数形参，
	 * 新建子表时应将这部分符号加入其中，之后将其设为null */
	private ArrayList<Symbol> innerSymbols;
	/* 当前位于的函数，插入Function型符号表项时更新为插入的函数；
	 * 离开块作用域时，更新为外层符号表的function */
	private Function currentFunction;

	public SymbolTable() {
		current = new SubSymbolTable(null, null);
	}

	/**
	 * 新建子表，每个作用域应建立一张子表
	 */
	public void create() {
		SubSymbolTable newTable = new SubSymbolTable(current, currentFunction);
		current.addInner(newTable);
		current = newTable;

		// 将innerSymbols中的符号加入表中
		if (innerSymbols != null) {
			innerSymbols.forEach(this::insert);
			innerSymbols = null;
		}
	}

	/**
	 * 切换当前符号表至外层符号表，退出一个作用域时使用
	 */
	public void changeToOuter() {
		current = current.getOuter();
		currentFunction = current.getFunction();
	}

	/**
	 * 保留形参，以便新建子表时加入子表中
	 */
	public void saveParams(ArrayList<Symbol> params) {
		this.innerSymbols = params;
	}

	/**
	 * 符号表中搜索标识符，填符号表的过程中使用
	 * @param name 标识符名字
	 * @return 对应的符号表项Symbol，无定义时返回为null
	 */
	public Symbol search(String name) {
		return current.search(name);
	}

	/**
	 * 将标识符插入当前符号表
	 * @param symbol 要插入的标识符
	 * @return 是否插入成功，若为false，表示重复定义，即表内已有该标识符
	 */
	public boolean insert(Symbol symbol) {
		boolean success = current.add(symbol);
		if (symbol instanceof Function) {
			currentFunction = (Function) symbol;
		}
		return success;
	}

	/**
	 * 获取当前表所在函数
	 * @return 当前表所在函数
	 */
	public Function getCurrentFunction() {
		return currentFunction;
	}
}
