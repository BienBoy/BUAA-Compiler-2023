package SymbolTable;

public class Array1D extends ConstSymbol {
	private int shape; // 数组形状，即长度
	private Integer[] value; // 常量数组的值

	public Array1D(String name) {
		super(name, false);
	}

	public Array1D(String name, int shape) {
		super(name, false);
		this.shape = shape;
	}

	public Array1D(String name, int shape, Integer[] value, boolean constant) {
		super(name, constant);
		this.shape = shape;
		this.value = value;
	}

	public Integer getValue(int i) {
		if (value == null || value[i] == null)
			return isGlobal() ? 0 : null;
		return value[i];
	}

	public Integer[] getValue() {
		return value;
	}

	public int getShape() {
		return shape;
	}
}
