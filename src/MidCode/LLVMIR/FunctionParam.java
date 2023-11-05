package MidCode.LLVMIR;

import SymbolTable.Array1D;
import SymbolTable.Array2D;
import SymbolTable.Symbol;
import SymbolTable.Variable;

public class FunctionParam extends Value {
	public FunctionParam(String name, Symbol symbol) {
		super(name, symbol);
	}

	@Override
	public String getType() {
		if (symbol instanceof Variable) {
			return "i32";
		}
		if (symbol instanceof Array1D) {
			Array1D array = (Array1D) symbol;
			if (array.getShape() > 0) {
				return "[" + array.getShape() + " x i32]";
			} else {
				return "i32*";
			}
		}
		Array2D array = (Array2D) symbol;
		if (array.getShapeX() > 0) {
			return "[" + array.getShapeX() + " x [" + array.getShapeY() + " x i32]]";
		} else {
			return "[" + array.getShapeY() + " x i32]*";
		}
	}
}
