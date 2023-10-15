package SymbolTable;

public class Array2D extends ConstSymbol {
	private int shapeX; // 数组第一维大小
	private int shapeY; // 数组第二维大小
	private int[][] value; // 常量数组的值

	public Array2D(String name, int shapeY) {
		super(name, false);
		this.shapeY = shapeY;
	}

	public Array2D(String name, int shapeX, int shapeY) {
		super(name, false);
		this.shapeX = shapeX;
		this.shapeY = shapeY;
	}

	public Array2D(String name, int shapeX, int shapeY, int[][] value) {
		super(name, true);
		this.shapeX = shapeX;
		this.shapeY = shapeY;
		this.value = value;
	}

	public int getValue(int i, int j) {
		return value[i][j];
	}

	public int getShapeX() {
		return shapeX;
	}

	public int getShapeY() {
		return shapeY;
	}
}
