package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Function;
import MidCode.LLVMIR.Value;
import SymbolTable.Array1D;
import SymbolTable.Array2D;
import SymbolTable.Symbol;
import SymbolTable.Variable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Call extends Instruction {
	public Call(String name, Value... operands) {
		super(name, operands);
	}

	public Call(Value... operands) {
		super(operands);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		Function function = (Function) operands.get(0);
		SymbolTable.Function symbol = (SymbolTable.Function) function.getSymbol();
		if (symbol.hasReturn()) {
			writer.write("\t" + name + " = call i32 ");
		} else {
			writer.write("\tcall void ");
		}

		writer.write(function.getName() + "(");

		ArrayList<Symbol> params = symbol.getParams();
		for (int i = 1; i < operands.size(); i++) {
			Value operand = operands.get(i);
			if (params.get(i - 1) instanceof Variable) {
				writer.write("i32 " + operand.getName());
			} else if (params.get(i - 1) instanceof Array1D) {
				writer.write("i32* " + operand.getName());
			} else {
				int shapeY = ((Array2D) params.get(i - 1)).getShapeY();
				writer.write("[" + shapeY + " x i32]* " + operand.getName());
			}

			if (i < operands.size() - 1) {
				writer.write(", ");
			}
		}
		
		writer.write(")");
		writer.newLine();
	}

	@Override
	public boolean hasResult() {
		Function function = (Function) operands.get(0);
		SymbolTable.Function symbol = (SymbolTable.Function) function.getSymbol();
		return symbol.hasReturn();
	}
}
