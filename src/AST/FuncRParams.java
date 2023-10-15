package AST;

import java.util.ArrayList;

public class FuncRParams extends BranchNode {
	/**
	 * 获取实参数量
	 *
	 * @return 实参数量
	 */
	public int getParamsNum() {
		return (children.size() + 1) / 2;
	}

	public ArrayList<Class<?>> getParamsType() {
		ArrayList<Class<?>> paramsType = new ArrayList<>();
		for (int i = 0; i < children.size(); i += 2) {
			paramsType.add(((TypeAvailable) children.get(i)).getType());
		}
		return paramsType;
	}
}
