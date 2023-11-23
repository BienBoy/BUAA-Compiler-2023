package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.FunctionParam;
import MidCode.LLVMIR.Value;
import SymbolTable.Array1D;
import SymbolTable.Array2D;

import java.io.BufferedWriter;
import java.io.IOException;

public class Getelementptr extends Instruction {
	public Getelementptr(String name, Value... operands) {
		super(name, operands);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		String type = operands.get(0).getType();
		writer.write("\t" + name + " = getelementptr ");
		writer.write(getSourceType() + ", " + type + " " + operands.get(0).getName());
		for (int i = 1; i < operands.size(); i++) {
			writer.write(", ");
			writer.write(operands.get(i).getType() + " " + operands.get(i).getName());
		}
		writer.newLine();
	}

	public String getSourceType() {
		String type = operands.get(0).getType();
		return type.substring(0, type.length() - 1);
	}

	@Override
	public String getType() {
		String type = getSourceType();
		if (operands.size() >= 3) {
			for (int i = 0; i < type.length(); i++) {
				if (type.charAt(i) == 'x') {
					type = type.substring(i + 1, type.length() - 1);
					break;
				}
			}
		}
		if (operands.size() == 4) {
			for (int i = 0; i < type.length(); i++) {
				if (type.charAt(i) == 'x') {
					type = type.substring(i + 1, type.length() - 1);
					break;
				}
			}
		}
		return type.trim() + "*";
	}

	// 减少的层数
	public int reducedLayer() {
		return operands.size() - 1;
	}

	// 获取指定层的大小
	public int getSize(int layer) {
		Value operand = operands.get(0);
		if (operand instanceof Load || operand instanceof FunctionParam) {
			// 操作对象为指针
			Value value = null;
			if (operand instanceof Load) {
				value = ((Load) operand).getOperands().get(0);
			} else {
				value = operand;
			}
			if (value.getSymbol() instanceof Array1D) {
				// 操作对象为指针i32*，各层大小为：4
				if (layer == 0) {
					return 4;
				}
				throw new RuntimeException("getelementptr有误");
			} else if (value.getSymbol() instanceof Array2D) {
				// 操作对象为指针[shapeY x i32]*，各层大小为：4 * shapeY, 4
				Array2D array = (Array2D) value.getSymbol();
				if (layer == 0) {
					return 4 * array.getShapeY();
				} else if (layer == 1) {
					return 4;
				}
				throw new RuntimeException("getelementptr有误");
			}
			throw new RuntimeException("getelementptr有误");
		} else if (operand.getSymbol() instanceof Array1D) {
			Array1D array = (Array1D) operand.getSymbol();
			// 操作对象为数组，各层大小分别为：4 * shape、4
			if (layer == 0) {
				return 4 * array.getShape();
			} else if (layer == 1) {
				return 4;
			}
			throw new RuntimeException("getelementptr有误");
		} else if (operand.getSymbol() instanceof Array2D) {
			Array2D array = (Array2D) operand.getSymbol();
			// 操作对象为数组，各层大小分别为：4 * shapeX * shapeY、4 * shapeY、4
			if (layer == 0) {
				return 4 * array.getShapeX() * array.getShapeY();
			} else if (layer == 1) {
				return 4 * array.getShapeY();
			} else if (layer == 2) {
				return 4;
			}
			throw new RuntimeException("getelementptr有误");
		} else if (operand instanceof Getelementptr) {
			Getelementptr g = (Getelementptr) operand;
			return g.getSize(layer + g.reducedLayer() - 1);
		}
		throw new RuntimeException("getelementptr有误");
	}

	@Override
	public boolean hasResult() {
		return true;
	}
}
