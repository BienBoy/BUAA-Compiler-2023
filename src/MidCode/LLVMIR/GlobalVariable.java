package MidCode.LLVMIR;

import SymbolTable.Array1D;
import SymbolTable.Array2D;
import SymbolTable.Symbol;
import SymbolTable.Variable;

import java.io.BufferedWriter;
import java.io.IOException;

public class GlobalVariable extends Value {
	public GlobalVariable(String name, Symbol symbol) {
		super(name, symbol);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		writer.write(name + " = dso_local ");
		if (symbol instanceof Variable) {
			// 普通变量
			if (((Variable) symbol).isConstant()) {
				writer.write("constant i32 ");
			} else {
				writer.write("global i32 ");
			}

			// 获取初始值
			Integer initValue = ((Variable) symbol).getValue();
			if (initValue == null) {
				initValue = 0;
			}
			writer.write(initValue + "");
			writer.newLine();
		} else if (symbol instanceof Array1D) {
			Array1D array = (Array1D) symbol;
			if (array.isConstant()) {
				writer.write("constant ");
			} else {
				writer.write("global ");
			}
			writer.write("[" + array.getShape() + " x i32] ");
			Integer[] value = array.getValue();
			writer.write(get1DInitStr(value));
			writer.newLine();
		} else {
			Array2D array = (Array2D) symbol;
			if (array.isConstant()) {
				writer.write("constant ");
			} else {
				writer.write("global ");
			}
			writer.write("[" + array.getShapeX() + " x [" + array.getShapeY() + " x i32]] ");
			Integer[][] value = array.getValue();
			writer.write(get2DInitStr(value));
			writer.newLine();
		}
	}

	private String get1DInitStr(Integer[] value) {
		if (value == null) {
			return "zeroinitializer";
		}
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (int i = 0; i < value.length; i++) {
			if (value[i] == null) {
				builder.append("i32 ").append(0);
			} else {
				builder.append("i32 ").append(value[i]);
			}
			if (i < value.length - 1) {
				builder.append(", ");
			}
		}
		builder.append("]");
		return builder.toString();
	}

	private String get2DInitStr(Integer[][] value) {
		if (value == null) {
			return "zeroinitializer";
		}
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (int i = 0; i < value.length; i++) {
			builder.append("[").append(value[i].length).append(" x i32] ")
					.append(get1DInitStr(value[i]));
			if (i < value.length - 1) {
				builder.append(", ");
			}
		}
		builder.append("]");
		return builder.toString();
	}

	@Override
	public String getType() {
		if (symbol instanceof Variable) {
			return "i32*";
		}
		if (symbol instanceof Array1D) {
			Array1D array = (Array1D) symbol;
			return "[" + array.getShape() + " x i32]*";
		}
		Array2D array = (Array2D) symbol;
		return "[" + array.getShapeX() + " x [" + array.getShapeY() + " x i32]]*";
	}

	public String getMips() {
		StringBuilder builder = new StringBuilder();
		builder.append(getRawName()).append(": ");
		if (symbol instanceof Variable) {
			builder.append(".word ");
			Integer initValue = ((Variable) symbol).getValue();
			if (initValue == null) {
				initValue = 0;
			}
			builder.append(initValue.toString());
			return builder.toString();
		}
		if (symbol instanceof Array1D) {
			Array1D array = (Array1D) symbol;
			int length = array.getShape();
			Integer[] value = array.getValue();
			if (value == null) {
				builder.append(".space ");
				builder.append(String.valueOf(length * 4));
				return builder.toString();
			}
			builder.append(".word ");
			for (int i = 0; i < length; i++) {
				builder.append(value[i].toString());
				if (i < length - 1) {
					builder.append(",");
				}
			}
			return builder.toString();
		}
		Array2D array = (Array2D) symbol;
		int length1 = array.getShapeX();
		int length2 = array.getShapeY();
		Integer[][] value = array.getValue();
		if (value == null) {
			builder.append(".space ");
			builder.append(String.valueOf(length1 * length2 * 4));
			return builder.toString();
		}
		builder.append(".word ");
		for (int i = 0; i < length1; i++) {
			for (int j = 0; j < length2; j++) {
				if (value[i][j] == null) {
					builder.append("0");
				} else {
					builder.append(String.valueOf(value[i][j]));
				}
				if (j < length2 - 1) {
					builder.append(",");
				}
			}
			if (i < length1 - 1) {
				builder.append(",");
			}
		}
		return builder.toString();
	}
}