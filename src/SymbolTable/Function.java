package SymbolTable;

import java.util.ArrayList;

public class Function extends Symbol {
	private final boolean returnInt; // 是否有返回值，分别对应void和int
	private final ArrayList<Symbol> params; // 函数的形参

	public Function(String name, boolean returnInt, ArrayList<Symbol> params) {
		super(name);
		this.returnInt = returnInt;
		this.params = params;
	}

	/**
	 * 函数是否有返回值
	 * @return 函数是否有返回值
	 */
	public boolean hasReturn() {
		return returnInt;
	}

	/**
	 * 获取函数的形参列表
	 * @return 函数的形参列表
	 */
	public ArrayList<Symbol> getParams() {
		return params;
	}

	/**
	 * 判断实参与形参数量是否相同
	 * @param rParamsNum 形参类型列表
	 * @return 实参与形参数量是否相同
	 */
	public boolean matchNum(int rParamsNum) {
		if (params == null) {
			return rParamsNum == 0;
		}
		return rParamsNum == params.size();
	}

	/**
	 * 判断实参是否与形参类型匹配
	 * @param rParamsType 形参类型列表
	 * @return 实参是否与形参类型匹配
	 */
	public boolean matchType(ArrayList<Class<?>> rParamsType) {
		if (rParamsType == null) {
			return params == null || params.isEmpty();
		}
		if (rParamsType.size() != params.size()) {
			return false;
		}
		for (int i = 0; i < params.size(); i++) {
			if (params.get(i).getClass().equals(rParamsType.get(i))) {
				continue;
			}
			return false;
		}
		return true;
	}
}
