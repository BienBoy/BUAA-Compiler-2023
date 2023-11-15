package MidCode.LLVMIR.Instruction;

import SymbolTable.Array1D;
import SymbolTable.Array2D;
import SymbolTable.Variable;

import java.io.BufferedWriter;
import java.io.IOException;

public class Alloca extends Instruction {
	public Alloca(String name) {
		super(name);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		if (symbol instanceof Variable) {
			writer.write("\t" + name + " = alloca i32");
		} else if (symbol instanceof Array1D) {
			Array1D array = (Array1D) symbol;
			if (array.getShape() > 0) {
				writer.write("\t" + name + " = alloca [" + array.getShape() + " x i32]");
			} else {
				writer.write("\t" + name + " = alloca i32*");
			}
		} else {
			Array2D array = (Array2D) symbol;
			if (array.getShapeX() > 0) {
				writer.write("\t" + name + " = alloca [" + array.getShapeX() + " x [" + array.getShapeY() + " x i32]]");
			} else {
				writer.write("\t" + name + " = alloca [" + array.getShapeY() + " x i32]*");
			}
		}
		writer.newLine();
	}

	@Override
	public String getType() {
		if (symbol instanceof Variable) {
			return "i32*";
		}
		if (symbol instanceof Array1D) {
			Array1D array = (Array1D) symbol;
			if (array.getShape() > 0) {
				return "[" + array.getShape() + " x i32]*";
			} else {
				return "i32**";
			}
		}
		Array2D array = (Array2D) symbol;
		if (array.getShapeX() > 0) {
			return "[" + array.getShapeX() + " x [" + array.getShapeY() + " x i32]]*";
		} else {
			return "[" + array.getShapeY() + " x i32]**";
		}
	}

	public int getSize() {
		if (symbol instanceof Variable) {
			return 4;
		}
		if (symbol instanceof Array1D) {
			Array1D array = (Array1D) symbol;
			if (array.getShape() > 0) {
				return array.getShape() * 4;
			} else {
				return 4;
			}
		}
		Array2D array = (Array2D) symbol;
		if (array.getShapeX() > 0) {
			return array.getShapeX() * array.getShapeY() * 4;
		} else {
			return 4;
		}
	}

	public boolean isLocalVariable() {
		return symbol instanceof Variable || getType().endsWith("**");
	}
}
